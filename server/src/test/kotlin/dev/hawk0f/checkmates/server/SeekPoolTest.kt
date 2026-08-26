package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SeekPoolTest {

    private val blitz = TimeControl(180, 2)

    private fun newPool() = SeekPool(RoomRegistry("http://localhost"))

    @Test
    fun twoCompatibleSeeksArePairedIntoOneGame() = runTest {
        val pool = newPool()
        val (_, first) = pool.enqueue("Anna", 1L, blitz, 1500)
        assertFalse(first.isCompleted)

        val (_, second) = pool.enqueue("Boris", 2L, blitz, 1520)
        val hostMatch = first.await()
        val guestMatch = second.await()

        assertEquals(hostMatch.gameId, guestMatch.gameId)
        assertEquals(hostMatch.shortCode, guestMatch.shortCode)
        assertNotEquals(hostMatch.color, guestMatch.color)
        assertNotEquals(hostMatch.playerToken, guestMatch.playerToken)
        assertEquals("Boris", hostMatch.opponentName)
        assertEquals("Anna", guestMatch.opponentName)
    }

    @Test
    fun aSeekerWhoLeftIsNotPairedWithTheNextArrival() = runTest {
        val pool = newPool()
        val (_, gone) = pool.enqueue("Anna", 1L, blitz, 1500)
        gone.cancel()

        val (_, arriving) = pool.enqueue("Boris", 2L, blitz, 1500)

        assertFalse(arriving.isCompleted)
        assertEquals(1, pool.queuedFor(blitz))
    }

    @Test
    fun seeksWithDifferentTimeControlsAreNotPaired() = runTest {
        val pool = newPool()
        val (_, first) = pool.enqueue("Anna", 1L, blitz, 1500)
        val (_, second) = pool.enqueue("Boris", 2L, TimeControl(600, 0), 1500)
        assertFalse(first.isCompleted)
        assertFalse(second.isCompleted)
        assertEquals(1, pool.queuedFor(blitz))
    }

    @Test
    fun farApartRatingsWaitUntilTheWindowWidens() = runTest {
        val pool = newPool()
        val start = 1_000_000L
        val (_, first) = pool.enqueue("Anna", 1L, blitz, 1200, nowMillis = start)
        val (_, tooStrong) = pool.enqueue("Boris", 2L, blitz, 2100, nowMillis = start)
        assertFalse(first.isCompleted)
        assertFalse(tooStrong.isCompleted)

        val (_, late) = pool.enqueue("Clara", 3L, blitz, 2100, nowMillis = start + 60_000)
        assertTrue(first.isCompleted || tooStrong.isCompleted)
        assertTrue(late.isCompleted)
    }

    @Test
    fun theSameUserIsNeverPairedWithThemselves() = runTest {
        val pool = newPool()
        val (_, first) = pool.enqueue("Anna", 7L, blitz, 1500)
        val (_, second) = pool.enqueue("Anna", 7L, blitz, 1500)
        assertFalse(first.isCompleted)
        assertFalse(second.isCompleted)
        assertEquals(2, pool.queuedFor(blitz))
    }

    @Test
    fun cancelledSeeksAreRemovedFromTheQueue() = runTest {
        val pool = newPool()
        val (seekId, pending) = pool.enqueue("Anna", 1L, blitz, 1500)
        pool.cancel(seekId)
        assertEquals(0, pool.queuedFor(blitz))

        val (_, second) = pool.enqueue("Boris", 2L, blitz, 1500)
        assertFalse(pending.isCompleted)
        assertFalse(second.isCompleted)
    }

    @Test
    fun pairedGameIsPlayableImmediately() = runTest {
        val registry = RoomRegistry("http://localhost")
        val pool = SeekPool(registry)
        pool.enqueue("Anna", 1L, blitz, 1500)
        val (_, second) = pool.enqueue("Boris", 2L, blitz, 1500)
        val match = second.await()

        val room = registry.byId(match.gameId)!!
        assertEquals(RoomStatus.IN_PROGRESS, room.status)
        val snapshot = room.snapshot()
        assertEquals(2, snapshot.players.size)
        assertEquals(setOf(180_000L), snapshot.players.map { it.remainingMillis }.toSet())
        assertEquals(0L, snapshot.turnStartedAtMillis)
    }

    @Test
    fun aPairedGameWithOddsSeatsBothClocksSeparately() = runTest {
        val registry = RoomRegistry("http://localhost")
        val pool = SeekPool(registry)
        val odds = TimeControl(300, 0, blackInitialSeconds = 60)
        pool.enqueue("Anna", 1L, odds, 1500)
        val (_, second) = pool.enqueue("Boris", 2L, odds, 1500)
        val match = second.await()

        val snapshot = registry.byId(match.gameId)!!.snapshot()
        val clocks = snapshot.players.associate { it.color to it.remainingMillis }
        assertEquals(300_000L, clocks[PieceColor.WHITE])
        assertEquals(60_000L, clocks[PieceColor.BLACK])
    }
}
