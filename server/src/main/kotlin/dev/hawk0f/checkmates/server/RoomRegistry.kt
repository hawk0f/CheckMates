package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.ShortCode
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class CreatedGame(
    val gameId: String,
    val shortCode: String,
    val playerToken: String,
    val hostColor: PieceColor
)

class RoomRegistry(private val publicBaseUrl: String) {

    private val roomsById = ConcurrentHashMap<String, GameRoom>()
    private val idsByCode = ConcurrentHashMap<String, String>()

    fun joinUrl(shortCode: String): String = "$publicBaseUrl/game/$shortCode"

    fun create(hostName: String, timeControl: TimeControl? = null): CreatedGame {
        val gameId = UUID.randomUUID().toString()
        val playerToken = UUID.randomUUID().toString()
        val hostColor = if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
        while (true) {
            val code = ShortCode.generate()
            if (idsByCode.putIfAbsent(code, gameId) == null) {
                val room = GameRoom(gameId, code, playerToken, hostName, hostColor, timeControl)
                roomsById[gameId] = room
                return CreatedGame(gameId, code, playerToken, hostColor)
            }
        }
    }

    fun byId(gameId: String): GameRoom? = roomsById[gameId]

    fun byCode(code: String): GameRoom? = idsByCode[ShortCode.normalize(code)]?.let { roomsById[it] }

    suspend fun cleanup(nowMillis: Long = System.currentTimeMillis()) {
        for (room in roomsById.values.filter { it.isStale(nowMillis) }) {
            room.closeSessions()
            roomsById.remove(room.gameId)
            idsByCode.remove(room.shortCode)
        }
    }
}
