package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.SeekMessage
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SeekPool(private val registry: RoomRegistry) {

    private class Seek(
        val id: String,
        val name: String,
        val userId: Long?,
        val timeControl: TimeControl,
        val rating: Int,
        val createdAtMillis: Long,
        val result: CompletableDeferred<SeekMessage.Matched>
    )

    private val mutex = Mutex()
    private val waiting = mutableListOf<Seek>()

    val size: Int get() = waiting.size

    suspend fun enqueue(
        name: String,
        userId: Long?,
        timeControl: TimeControl,
        rating: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<String, CompletableDeferred<SeekMessage.Matched>> {
        val seek = Seek(
            id = UUID.randomUUID().toString(),
            name = name,
            userId = userId,
            timeControl = timeControl,
            rating = rating,
            createdAtMillis = nowMillis,
            result = CompletableDeferred()
        )
        while (true) {
            val opponent = mutex.withLock {
                waiting.removeAll { it.result.isCompleted }
                val candidate = waiting
                    .filter { it.compatibleWith(seek, nowMillis) }
                    .minByOrNull { abs(it.rating - seek.rating) }
                if (candidate != null) {
                    waiting.remove(candidate)
                } else {
                    waiting.add(seek)
                }
                candidate
            }
            if (opponent == null || pair(opponent, seek)) {
                break
            }
        }
        return seek.id to seek.result
    }

    suspend fun cancel(seekId: String) {
        mutex.withLock {
            waiting.removeAll { it.id == seekId }
        }
    }

    suspend fun queuedFor(timeControl: TimeControl): Int = mutex.withLock {
        waiting.count { it.timeControl == timeControl && !it.result.isCompleted }
    }

    private suspend fun pair(host: Seek, guest: Seek): Boolean {
        if (host.result.isCompleted) {
            return false
        }
        val created = registry.create(
            hostName = host.name,
            timeControl = host.timeControl,
            hostUserId = host.userId
        )
        val room = registry.byId(created.gameId)
        val guestToken = UUID.randomUUID().toString()
        val guestColor = room?.seatGuest(guestToken, guest.name, guest.userId)
        if (room == null || guestColor == null) {
            host.result.completeExceptionally(IllegalStateException("failed to create paired game"))
            guest.result.completeExceptionally(IllegalStateException("failed to create paired game"))
            return true
        }
        val hostAccepted = host.result.complete(
            SeekMessage.Matched(
                gameId = created.gameId,
                shortCode = created.shortCode,
                playerToken = created.playerToken,
                color = created.hostColor,
                opponentName = guest.name,
                opponentRating = guest.rating
            )
        )
        if (!hostAccepted) {
            registry.discard(created.gameId)
            return false
        }
        guest.result.complete(
            SeekMessage.Matched(
                gameId = created.gameId,
                shortCode = created.shortCode,
                playerToken = guestToken,
                color = guestColor,
                opponentName = host.name,
                opponentRating = host.rating
            )
        )
        return true
    }

    private fun Seek.compatibleWith(other: Seek, nowMillis: Long): Boolean {
        if (result.isCompleted) {
            return false
        }
        if (timeControl != other.timeControl) {
            return false
        }
        if (userId != null && userId == other.userId) {
            return false
        }
        val difference = abs(rating - other.rating)
        return difference <= window(nowMillis) || difference <= other.window(nowMillis)
    }

    private fun Seek.window(nowMillis: Long): Int {
        val waitedSeconds = ((nowMillis - createdAtMillis) / 1000).toInt().coerceAtLeast(0)
        return (BASE_WINDOW + waitedSeconds * WINDOW_GROWTH_PER_SECOND).coerceAtMost(MAX_WINDOW)
    }

    companion object {
        const val BASE_WINDOW = 120
        const val WINDOW_GROWTH_PER_SECOND = 25
        const val MAX_WINDOW = 900
    }
}
