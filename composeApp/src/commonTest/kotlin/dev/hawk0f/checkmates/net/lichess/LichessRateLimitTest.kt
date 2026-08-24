package dev.hawk0f.checkmates.net.lichess

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LichessRateLimitTest {

    @Test
    fun cooldownStartsClear() = runTest {
        LichessRateLimit.clearCooldown()
        assertTrue(LichessRateLimit.cooldownRemainingMillis == 0L)
    }

    @Test
    fun retryAfterHeaderSetsTheCooldown() = runTest {
        LichessRateLimit.clearCooldown()
        LichessRateLimit.noteRateLimited(2)
        val remaining = LichessRateLimit.cooldownRemainingMillis
        assertTrue(remaining in 1_000..2_000, "unexpected cooldown: $remaining")
    }

    @Test
    fun missingHeaderFallsBackToAMinute() = runTest {
        LichessRateLimit.clearCooldown()
        LichessRateLimit.noteRateLimited(null)
        val remaining = LichessRateLimit.cooldownRemainingMillis
        assertTrue(remaining in 55_000..60_000, "unexpected cooldown: $remaining")
    }

    @Test
    fun absurdRetryAfterIsCapped() = runTest {
        LichessRateLimit.clearCooldown()
        LichessRateLimit.noteRateLimited(86_400)
        val remaining = LichessRateLimit.cooldownRemainingMillis
        assertTrue(remaining in 500_000..600_000, "unexpected cooldown: $remaining")
    }
}
