package dev.hawk0f.checkmates.shared.protocol

import dev.hawk0f.checkmates.shared.domain.PieceColor

object ClockRules {

    fun initialMillis(timeControl: TimeControl, color: PieceColor): Long =
        timeControl.initialSecondsFor(color) * 1000L

    fun remainingAfterMove(
        remainingMillis: Long,
        elapsedMillis: Long,
        timeControl: TimeControl,
        color: PieceColor
    ): Long {
        val elapsed = elapsedMillis.coerceAtLeast(0)
        val bonusMillis = timeControl.incrementSecondsFor(color) * 1000L
        return when (timeControl.mode) {
            ClockMode.FISCHER -> remainingMillis - elapsed + bonusMillis
            ClockMode.BRONSTEIN -> remainingMillis - elapsed + minOf(elapsed, bonusMillis)
            ClockMode.DELAY -> remainingMillis - (elapsed - bonusMillis).coerceAtLeast(0)
        }
    }
}
