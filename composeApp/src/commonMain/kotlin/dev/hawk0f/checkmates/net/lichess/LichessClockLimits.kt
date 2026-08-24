package dev.hawk0f.checkmates.net.lichess

object LichessClockLimits {

    const val MIN_ESTIMATED_SECONDS = 480

    fun estimatedSeconds(limitSeconds: Int, incrementSeconds: Int): Int =
        limitSeconds + 40 * incrementSeconds

    fun isPlayable(limitSeconds: Int, incrementSeconds: Int, days: Int? = null): Boolean =
        days != null || estimatedSeconds(limitSeconds, incrementSeconds) >= MIN_ESTIMATED_SECONDS
}
