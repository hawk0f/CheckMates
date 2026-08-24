package dev.hawk0f.checkmates.server

import at.favre.lib.crypto.bcrypt.BCrypt
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import dev.hawk0f.checkmates.shared.protocol.GameRecordRequest
import dev.hawk0f.checkmates.shared.protocol.ProfileResponse
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

sealed interface AuthResult {
    data class Success(val token: String, val profile: ProfileResponse) : AuthResult
    data class Failure(val code: String, val message: String) : AuthResult
}

class UserRepository(private val database: Database) {

    private val random = SecureRandom()

    suspend fun register(login: String, password: String, displayName: String): AuthResult = dbQuery {
        val normalizedLogin = login.trim().lowercase()
        if (!normalizedLogin.matches(Regex("[a-z0-9_.-]{3,20}"))) {
            return@dbQuery AuthResult.Failure("BAD_LOGIN", "login must be 3-20 chars: a-z, 0-9, _ . -")
        }
        if (password.length < 6) {
            return@dbQuery AuthResult.Failure("BAD_PASSWORD", "password must be at least 6 chars")
        }
        val exists = Users.selectAll().where { Users.login eq normalizedLogin }.any()
        if (exists) {
            return@dbQuery AuthResult.Failure("LOGIN_TAKEN", "login already taken")
        }
        val name = displayName.trim().take(40).ifEmpty { normalizedLogin }
        val now = System.currentTimeMillis()
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val userId = Users.insert {
            it[Users.login] = normalizedLogin
            it[passwordHash] = hash
            it[Users.displayName] = name
            it[avatarKind] = "piece"
            it[avatarValue] = "wn"
            it[createdAtMillis] = now
        } get Users.id
        val token = issueTokenInTransaction(userId)
        AuthResult.Success(token, profileRow(userId)!!)
    }

    suspend fun login(login: String, password: String): AuthResult = dbQuery {
        val normalizedLogin = login.trim().lowercase()
        val row = Users.selectAll().where { Users.login eq normalizedLogin }.firstOrNull()
            ?: return@dbQuery AuthResult.Failure("BAD_CREDENTIALS", "wrong login or password")
        val verified = BCrypt.verifyer().verify(password.toCharArray(), row[Users.passwordHash]).verified
        if (!verified) {
            return@dbQuery AuthResult.Failure("BAD_CREDENTIALS", "wrong login or password")
        }
        val userId = row[Users.id]
        val token = issueTokenInTransaction(userId)
        AuthResult.Success(token, profileRow(userId)!!)
    }

    suspend fun userIdByToken(token: String, nowMillis: Long = System.currentTimeMillis()): Long? = dbQuery {
        val row = AuthSessions.selectAll().where { AuthSessions.token eq token }.firstOrNull()
            ?: return@dbQuery null
        val expiresAt = row[AuthSessions.expiresAtMillis]
            .takeIf { it > 0 }
            ?: (row[AuthSessions.createdAtMillis] + SESSION_TTL_MILLIS)
        if (expiresAt <= nowMillis) {
            AuthSessions.deleteWhere { AuthSessions.token eq token }
            return@dbQuery null
        }
        if (expiresAt - nowMillis < SESSION_REFRESH_THRESHOLD_MILLIS) {
            AuthSessions.update({ AuthSessions.token eq token }) {
                it[expiresAtMillis] = nowMillis + SESSION_TTL_MILLIS
            }
        }
        row[AuthSessions.userId]
    }

    suspend fun purgeExpiredSessions(nowMillis: Long = System.currentTimeMillis()): Int = dbQuery {
        val expired = AuthSessions.deleteWhere {
            (expiresAtMillis greater 0L) and (expiresAtMillis lessEq nowMillis)
        }
        val legacy = AuthSessions.deleteWhere {
            (expiresAtMillis eq 0L) and (createdAtMillis lessEq nowMillis - SESSION_TTL_MILLIS)
        }
        expired + legacy
    }

    suspend fun logout(token: String): Unit = dbQuery {
        AuthSessions.deleteWhere { AuthSessions.token eq token }
        Unit
    }

    suspend fun profile(userId: Long): ProfileResponse? = dbQuery {
        profileRow(userId)
    }

    suspend fun updateProfile(
        userId: Long,
        displayName: String?,
        avatarKind: String?,
        avatarValue: String?
    ): ProfileResponse? = dbQuery {
        Users.update({ Users.id eq userId }) { statement ->
            displayName?.trim()?.take(40)?.takeIf { it.isNotEmpty() }?.let { statement[Users.displayName] = it }
            avatarKind?.takeIf { it == "piece" || it == "emoji" }?.let { statement[Users.avatarKind] = it }
            avatarValue?.trim()?.take(20)?.takeIf { it.isNotEmpty() }?.let { statement[Users.avatarValue] = it }
        }
        profileRow(userId)
    }

    suspend fun insertGame(userId: Long, request: GameRecordRequest): Long = dbQuery {
        GameRecords.insert {
            it[GameRecords.userId] = userId
            it[mode] = request.mode.take(10)
            it[myColor] = request.myColor?.name
            it[whiteName] = request.whiteName.take(40)
            it[blackName] = request.blackName.take(40)
            it[winner] = request.winner?.name
            it[reason] = request.reason.name
            it[uciHistory] = request.uciHistory.joinToString(" ")
            it[finishedAtMillis] = request.finishedAtMillis
        } get GameRecords.id
    }

    suspend fun countUsers(): Long = dbQuery { Users.selectAll().count() }

    suspend fun countGames(): Long = dbQuery { GameRecords.selectAll().count() }

    suspend fun listGames(userId: Long, limit: Int = 200): List<GameHistoryItem> = dbQuery {
        GameRecords.selectAll()
            .where { GameRecords.userId eq userId }
            .orderBy(GameRecords.finishedAtMillis, SortOrder.DESC)
            .limit(limit)
            .map { it.toHistoryItem() }
    }

    private fun issueTokenInTransaction(userId: Long): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val token = bytes.joinToString("") { byte -> "%02x".format(byte) }
        val now = System.currentTimeMillis()
        AuthSessions.insert {
            it[AuthSessions.token] = token
            it[AuthSessions.userId] = userId
            it[createdAtMillis] = now
            it[expiresAtMillis] = now + SESSION_TTL_MILLIS
        }
        return token
    }

    private fun profileRow(userId: Long): ProfileResponse? =
        Users.selectAll().where { Users.id eq userId }.firstOrNull()?.let { row ->
            ProfileResponse(
                id = row[Users.id],
                login = row[Users.login],
                displayName = row[Users.displayName],
                avatarKind = row[Users.avatarKind],
                avatarValue = row[Users.avatarValue],
                createdAtMillis = row[Users.createdAtMillis]
            )
        }

    private fun ResultRow.toHistoryItem() = GameHistoryItem(
        id = this[GameRecords.id],
        mode = this[GameRecords.mode],
        myColor = this[GameRecords.myColor]?.let { PieceColor.valueOf(it) },
        whiteName = this[GameRecords.whiteName],
        blackName = this[GameRecords.blackName],
        winner = this[GameRecords.winner]?.let { PieceColor.valueOf(it) },
        reason = GameOverReason.valueOf(this[GameRecords.reason]),
        uciHistory = this[GameRecords.uciHistory].split(" ").filter { it.isNotEmpty() },
        finishedAtMillis = this[GameRecords.finishedAtMillis]
    )

    private suspend fun <T> dbQuery(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction(database) { block() }
    }

    companion object {
        const val SESSION_TTL_MILLIS = 30L * 24 * 60 * 60 * 1000
        const val SESSION_REFRESH_THRESHOLD_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
