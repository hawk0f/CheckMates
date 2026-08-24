package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.ClockMode
import dev.hawk0f.checkmates.shared.protocol.ClockRules
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClockRulesTest {

    private val fischer = TimeControl(180, 5)
    private val bronstein = TimeControl(180, 5, ClockMode.BRONSTEIN)
    private val delay = TimeControl(180, 5, ClockMode.DELAY)

    @Test
    fun fischerAlwaysAddsTheFullIncrement() {
        assertEquals(
            180_000 - 1_000 + 5_000,
            ClockRules.remainingAfterMove(180_000, 1_000, fischer, PieceColor.WHITE)
        )
    }

    @Test
    fun bronsteinOnlyGivesBackWhatWasSpent() {
        assertEquals(
            180_000L,
            ClockRules.remainingAfterMove(180_000, 1_000, bronstein, PieceColor.WHITE)
        )
        assertEquals(
            180_000 - 8_000 + 5_000,
            ClockRules.remainingAfterMove(180_000, 8_000, bronstein, PieceColor.WHITE)
        )
    }

    @Test
    fun simpleDelayBurnsNothingInsideTheDelayWindow() {
        assertEquals(180_000L, ClockRules.remainingAfterMove(180_000, 4_000, delay, PieceColor.WHITE))
        assertEquals(180_000L - 3_000, ClockRules.remainingAfterMove(180_000, 8_000, delay, PieceColor.WHITE))
    }

    @Test
    fun bronsteinAndDelayNeverGrowTheClock() {
        for (elapsed in listOf(0L, 1_000L, 5_000L, 20_000L)) {
            assertTrue(ClockRules.remainingAfterMove(180_000, elapsed, bronstein, PieceColor.WHITE) <= 180_000)
            assertTrue(ClockRules.remainingAfterMove(180_000, elapsed, delay, PieceColor.WHITE) <= 180_000)
        }
    }

    @Test
    fun timeOddsGiveEachSideItsOwnClock() {
        val odds = TimeControl(300, 0, blackInitialSeconds = 60, blackIncrementSeconds = 3)
        assertEquals(300_000L, ClockRules.initialMillis(odds, PieceColor.WHITE))
        assertEquals(60_000L, ClockRules.initialMillis(odds, PieceColor.BLACK))
        assertEquals(
            300_000L - 2_000,
            ClockRules.remainingAfterMove(300_000, 2_000, odds, PieceColor.WHITE)
        )
        assertEquals(
            60_000L - 2_000 + 3_000,
            ClockRules.remainingAfterMove(60_000, 2_000, odds, PieceColor.BLACK)
        )
        assertTrue(odds.hasOdds)
    }

    @Test
    fun labelsCarryTheClockMode() {
        assertEquals("3+5", fischer.label)
        assertEquals("3+5 B", bronstein.label)
        assertEquals("3+5 D", delay.label)
        assertTrue(TimeControl(300, 0, blackInitialSeconds = 60).label.endsWith("⚖"))
    }

    @Test
    fun negativeElapsedIsIgnored() {
        assertEquals(
            180_000L + 5_000,
            ClockRules.remainingAfterMove(180_000, -500, fischer, PieceColor.WHITE)
        )
    }
}
