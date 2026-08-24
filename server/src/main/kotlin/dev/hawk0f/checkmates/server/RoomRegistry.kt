package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.ShortCode
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.random.asKotlinRandom

data class CreatedGame(
    val gameId: String,
    val shortCode: String,
    val playerToken: String,
    val hostColor: PieceColor
)

class RoomRegistry(
    private val publicBaseUrl: String,
    private val recorder: GameRecorder? = null,
    private val store: RoomStore = NoopRoomStore,
    private val ratings: RatingUpdater? = null
) {

    private val roomsById = ConcurrentHashMap<String, GameRoom>()
    private val idsByCode = ConcurrentHashMap<String, String>()
    private val secureRandom: Random = SecureRandom().asKotlinRandom()

    fun joinUrl(shortCode: String): String = "$publicBaseUrl/game/$shortCode"

    fun create(hostName: String, timeControl: TimeControl? = null, hostUserId: Long? = null): CreatedGame {
        val gameId = UUID.randomUUID().toString()
        val playerToken = UUID.randomUUID().toString()
        val hostColor = if (secureRandom.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
        while (true) {
            val code = ShortCode.generate(secureRandom)
            if (idsByCode.putIfAbsent(code, gameId) == null) {
                val room = GameRoom(
                    gameId = gameId,
                    shortCode = code,
                    hostToken = playerToken,
                    hostName = hostName,
                    hostColor = hostColor,
                    timeControl = timeControl,
                    hostUserId = hostUserId,
                    recorder = recorder,
                    ratings = ratings,
                    store = store
                )
                roomsById[gameId] = room
                return CreatedGame(gameId, code, playerToken, hostColor)
            }
        }
    }

    suspend fun restoreFromStore(): Int {
        val snapshots = runCatching { store.loadResumable() }.getOrElse { emptyList() }
        var restored = 0
        for (snapshot in snapshots) {
            if (roomsById.containsKey(snapshot.gameId)) {
                continue
            }
            val host = snapshot.players.firstOrNull() ?: continue
            val room = GameRoom(
                gameId = snapshot.gameId,
                shortCode = snapshot.shortCode,
                hostToken = host.token,
                hostName = host.name,
                hostColor = host.color,
                timeControl = snapshot.timeControl,
                hostUserId = host.userId,
                recorder = recorder,
                ratings = ratings,
                store = store
            )
            room.restoreFrom(snapshot)
            roomsById[snapshot.gameId] = room
            idsByCode[snapshot.shortCode] = snapshot.gameId
            restored++
        }
        return restored
    }

    val size: Int get() = roomsById.size

    fun countInProgress(): Int = roomsById.values.count { it.status == RoomStatus.IN_PROGRESS }

    fun byId(gameId: String): GameRoom? = roomsById[gameId]

    fun byCode(code: String): GameRoom? = idsByCode[ShortCode.normalize(code)]?.let { roomsById[it] }

    suspend fun cleanup(nowMillis: Long = System.currentTimeMillis()) {
        for (room in roomsById.values.filter { it.isStale(nowMillis) }) {
            room.closeSessions()
            roomsById.remove(room.gameId)
            idsByCode.remove(room.shortCode)
            runCatching { store.delete(room.gameId) }
        }
        runCatching { store.purgeExpired(nowMillis) }
    }
}
