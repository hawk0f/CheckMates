package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.protocol.CorrespondenceGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

sealed interface CorrespondenceMoveResult {
    data class Success(val game: CorrespondenceGame, val opponentUserId: Long) : CorrespondenceMoveResult
    data object NotFound : CorrespondenceMoveResult
    data object NotYourTurn : CorrespondenceMoveResult
    data object IllegalMove : CorrespondenceMoveResult
    data object Finished : CorrespondenceMoveResult
}

class CorrespondenceRepository(private val database: Database) {

    suspend fun create(whiteUserId: Long, blackUserId: Long): CorrespondenceGame? = dbQuery {
        val white = userName(whiteUserId) ?: return@dbQuery null
        val black = userName(blackUserId) ?: return@dbQuery null
        val id = CorrespondenceGames.insert {
            it[CorrespondenceGames.whiteUserId] = whiteUserId
            it[CorrespondenceGames.blackUserId] = blackUserId
            it[whiteName] = white
            it[blackName] = black
            it[updatedAtMillis] = System.currentTimeMillis()
        } get CorrespondenceGames.id
        row(id)?.toGame()
    }

    suspend fun list(userId: Long): List<CorrespondenceGame> = dbQuery {
        CorrespondenceGames.selectAll()
            .where { (CorrespondenceGames.whiteUserId eq userId) or (CorrespondenceGames.blackUserId eq userId) }
            .orderBy(CorrespondenceGames.updatedAtMillis, SortOrder.DESC)
            .limit(100)
            .map { it.toGame() }
    }

    suspend fun move(gameId: Long, userId: Long, uci: String): CorrespondenceMoveResult = dbQuery {
        val row = row(gameId) ?: return@dbQuery CorrespondenceMoveResult.NotFound
        if (row[CorrespondenceGames.reason] != null) {
            return@dbQuery CorrespondenceMoveResult.Finished
        }
        val whiteUserId = row[CorrespondenceGames.whiteUserId]
        val blackUserId = row[CorrespondenceGames.blackUserId]
        if (userId != whiteUserId && userId != blackUserId) {
            return@dbQuery CorrespondenceMoveResult.NotFound
        }
        val game = ChessGame()
        val history = row[CorrespondenceGames.uciHistory].split(' ').filter { it.isNotBlank() }
        for (move in history) {
            if (game.applyUci(move) !is MoveOutcome.Applied) {
                return@dbQuery CorrespondenceMoveResult.IllegalMove
            }
        }
        val expectedUser = if (game.sideToMove() == dev.hawk0f.checkmates.shared.domain.PieceColor.WHITE) {
            whiteUserId
        } else {
            blackUserId
        }
        if (expectedUser != userId) {
            return@dbQuery CorrespondenceMoveResult.NotYourTurn
        }
        if (game.applyUci(uci.lowercase()) !is MoveOutcome.Applied) {
            return@dbQuery CorrespondenceMoveResult.IllegalMove
        }
        val state = game.state()
        CorrespondenceGames.update({ CorrespondenceGames.id eq gameId }) {
            it[uciHistory] = state.uciHistory.joinToString(" ")
            it[winner] = state.result?.winner?.name
            it[reason] = state.result?.reason?.name
            it[updatedAtMillis] = System.currentTimeMillis()
        }
        val updated = row(gameId)?.toGame() ?: return@dbQuery CorrespondenceMoveResult.NotFound
        val opponent = if (userId == whiteUserId) {
            blackUserId
        } else {
            whiteUserId
        }
        CorrespondenceMoveResult.Success(updated, opponent)
    }

    private fun row(id: Long): ResultRow? =
        CorrespondenceGames.selectAll().where { CorrespondenceGames.id eq id }.firstOrNull()

    private fun userName(id: Long): String? =
        Users.selectAll().where { Users.id eq id }.firstOrNull()?.get(Users.displayName)

    private fun ResultRow.toGame(): CorrespondenceGame {
        val history = this[CorrespondenceGames.uciHistory].split(' ').filter { it.isNotBlank() }
        val game = ChessGame()
        history.forEach { game.applyUci(it) }
        return CorrespondenceGame(
            id = this[CorrespondenceGames.id],
            whiteUserId = this[CorrespondenceGames.whiteUserId],
            blackUserId = this[CorrespondenceGames.blackUserId],
            whiteName = this[CorrespondenceGames.whiteName],
            blackName = this[CorrespondenceGames.blackName],
            uciHistory = history,
            sideToMove = game.sideToMove(),
            winner = this[CorrespondenceGames.winner]?.let(dev.hawk0f.checkmates.shared.domain.PieceColor::valueOf),
            reason = this[CorrespondenceGames.reason]?.let(dev.hawk0f.checkmates.shared.domain.GameOverReason::valueOf),
            updatedAtMillis = this[CorrespondenceGames.updatedAtMillis]
        )
    }

    private suspend fun <T> dbQuery(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction(database) { block() }
    }
}
