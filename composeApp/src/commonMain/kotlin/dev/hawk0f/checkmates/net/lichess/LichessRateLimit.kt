package dev.hawk0f.checkmates.net.lichess

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object LichessRateLimit {

    const val DEFAULT_COOLDOWN_MILLIS = 60_000L
    private const val MAX_COOLDOWN_MILLIS = 10 * 60_000L

    private val mutex = Mutex()

    @Volatile
    private var blockedUntilMillis = 0L

    val cooldownRemainingMillis: Long
        get() = (blockedUntilMillis - nowMillis()).coerceAtLeast(0)

    suspend fun awaitReady() {
        while (true) {
            val waitMillis = mutex.withLock { (blockedUntilMillis - nowMillis()).coerceAtLeast(0) }
            if (waitMillis <= 0) {
                return
            }
            delay(waitMillis)
        }
    }

    suspend fun noteRateLimited(retryAfterSeconds: Long?) {
        val cooldown = retryAfterSeconds
            ?.times(1000)
            ?.coerceIn(1000, MAX_COOLDOWN_MILLIS)
            ?: DEFAULT_COOLDOWN_MILLIS
        mutex.withLock {
            blockedUntilMillis = maxOf(blockedUntilMillis, nowMillis() + cooldown)
        }
    }

    internal suspend fun clearCooldown() {
        mutex.withLock { blockedUntilMillis = 0 }
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}

val LichessThrottle = createClientPlugin("LichessThrottle") {
    onRequest { _, _ ->
        LichessRateLimit.awaitReady()
    }
    onResponse { response ->
        if (response.status == HttpStatusCode.TooManyRequests) {
            LichessRateLimit.noteRateLimited(response.headers[HttpHeaders.RetryAfter]?.toLongOrNull())
        }
    }
}
