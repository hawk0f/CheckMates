package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.ClockMode
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

data class RoomPlayerSnapshot(
    val color: PieceColor,
    val token: String,
    val name: String,
    val userId: Long?,
    val remainingMillis: Long?
)

data class RoomSnapshot(
    val gameId: String,
    val shortCode: String,
    val status: RoomStatus,
    val timeControl: TimeControl?,
    val uciHistory: List<String>,
    val players: List<RoomPlayerSnapshot>,
    val turnStartedAtMillis: Long,
    val lastActivityMillis: Long
)

interface RoomStore {
    suspend fun save(snapshot: RoomSnapshot)
    suspend fun delete(gameId: String)
    suspend fun loadResumable(nowMillis: Long = System.currentTimeMillis()): List<RoomSnapshot>
    suspend fun purgeExpired(nowMillis: Long = System.currentTimeMillis()): Int
}

object NoopRoomStore : RoomStore {
    override suspend fun save(snapshot: RoomSnapshot) = Unit
    override suspend fun delete(gameId: String) = Unit
    override suspend fun loadResumable(nowMillis: Long): List<RoomSnapshot> = emptyList()
    override suspend fun purgeExpired(nowMillis: Long): Int = 0
}

class SqliteRoomStore(private val database: Database) : RoomStore {

    override suspend fun save(snapshot: RoomSnapshot): Unit = dbQuery {
        GameRooms.upsert(GameRooms.gameId) { row ->
            row[gameId] = snapshot.gameId
            row[shortCode] = snapshot.shortCode
            row[status] = snapshot.status.name
            row[initialSeconds] = snapshot.timeControl?.initialSeconds ?: -1
            row[incrementSeconds] = snapshot.timeControl?.incrementSeconds ?: -1
            row[clockMode] = (snapshot.timeControl?.mode ?: ClockMode.FISCHER).id
            row[blackInitialSeconds] = snapshot.timeControl?.blackInitialSeconds ?: -1
            row[blackIncrementSeconds] = snapshot.timeControl?.blackIncrementSeconds ?: -1
            row[uciHistory] = snapshot.uciHistory.joinToString(" ")
            row[turnStartedAtMillis] = snapshot.turnStartedAtMillis
            row[lastActivityMillis] = snapshot.lastActivityMillis
            val white = snapshot.players.find { it.color == PieceColor.WHITE }
            val black = snapshot.players.find { it.color == PieceColor.BLACK }
            row[whiteToken] = white?.token
            row[whiteName] = white?.name
            row[whiteUserId] = white?.userId
            row[whiteMillis] = white?.remainingMillis ?: -1
            row[blackToken] = black?.token
            row[blackName] = black?.name
            row[blackUserId] = black?.userId
            row[blackMillis] = black?.remainingMillis ?: -1
        }
    }

    override suspend fun delete(gameId: String): Unit = dbQuery {
        GameRooms.deleteWhere { GameRooms.gameId eq gameId }
    }

    override suspend fun loadResumable(nowMillis: Long): List<RoomSnapshot> = dbQuery {
        GameRooms.selectAll()
            .where { GameRooms.status eq RoomStatus.IN_PROGRESS.name }
            .mapNotNull { row ->
                if (nowMillis - row[GameRooms.lastActivityMillis] > RESUMABLE_WINDOW_MILLIS) {
                    return@mapNotNull null
                }
                val players = buildList {
                    row[GameRooms.whiteToken]?.let { token ->
                        add(
                            RoomPlayerSnapshot(
                                color = PieceColor.WHITE,
                                token = token,
                                name = row[GameRooms.whiteName] ?: "White",
                                userId = row[GameRooms.whiteUserId],
                                remainingMillis = row[GameRooms.whiteMillis].takeIf { it >= 0 }
                            )
                        )
                    }
                    row[GameRooms.blackToken]?.let { token ->
                        add(
                            RoomPlayerSnapshot(
                                color = PieceColor.BLACK,
                                token = token,
                                name = row[GameRooms.blackName] ?: "Black",
                                userId = row[GameRooms.blackUserId],
                                remainingMillis = row[GameRooms.blackMillis].takeIf { it >= 0 }
                            )
                        )
                    }
                }
                if (players.size < 2) {
                    return@mapNotNull null
                }
                val initial = row[GameRooms.initialSeconds]
                val increment = row[GameRooms.incrementSeconds]
                RoomSnapshot(
                    gameId = row[GameRooms.gameId],
                    shortCode = row[GameRooms.shortCode],
                    status = RoomStatus.IN_PROGRESS,
                    timeControl = if (initial >= 0 && increment >= 0) {
                        TimeControl(
                            initialSeconds = initial,
                            incrementSeconds = increment,
                            mode = ClockMode.byId(row[GameRooms.clockMode]),
                            blackInitialSeconds = row[GameRooms.blackInitialSeconds].takeIf { it >= 0 },
                            blackIncrementSeconds = row[GameRooms.blackIncrementSeconds].takeIf { it >= 0 }
                        )
                    } else {
                        null
                    },
                    uciHistory = row[GameRooms.uciHistory].split(" ").filter { it.isNotBlank() },
                    players = players,
                    turnStartedAtMillis = row[GameRooms.turnStartedAtMillis],
                    lastActivityMillis = row[GameRooms.lastActivityMillis]
                )
            }
    }

    override suspend fun purgeExpired(nowMillis: Long): Int = dbQuery {
        GameRooms.deleteWhere { lastActivityMillis lessEq nowMillis - RESUMABLE_WINDOW_MILLIS }
    }

    private suspend fun <T> dbQuery(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction(database) { block() }
    }

    companion object {
        const val RESUMABLE_WINDOW_MILLIS = 24L * 60 * 60 * 1000
    }
}
