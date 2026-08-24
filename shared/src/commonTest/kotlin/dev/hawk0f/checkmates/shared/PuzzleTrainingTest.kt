package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.puzzle.BundledPuzzles
import dev.hawk0f.checkmates.shared.puzzle.PuzzleElo
import dev.hawk0f.checkmates.shared.puzzle.PuzzleProgress
import dev.hawk0f.checkmates.shared.puzzle.SpacedRepetition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PuzzleTrainingTest {

    private val now = 1_700_000_000_000L

    @Test
    fun solvingMovesThePuzzleUpABoxAndPushesItsDueDate() {
        val first = SpacedRepetition.onSolved(PuzzleProgress("cm-fork-1"), now)
        assertEquals(1, first.box)
        assertEquals(now + SpacedRepetition.intervalsMillis[1], first.dueAtMillis)
        assertEquals(1, first.solved)

        val second = SpacedRepetition.onSolved(first, now)
        assertEquals(2, second.box)
        assertTrue(second.dueAtMillis > first.dueAtMillis)
    }

    @Test
    fun theBoxNeverGrowsPastTheLongestInterval() {
        var progress = PuzzleProgress("cm-fork-1")
        repeat(20) { progress = SpacedRepetition.onSolved(progress, now) }
        assertEquals(SpacedRepetition.maxBox, progress.box)
    }

    @Test
    fun failingResetsThePuzzleToTheFirstBox() {
        val learned = SpacedRepetition.onSolved(SpacedRepetition.onSolved(PuzzleProgress("cm-fork-1"), now), now)
        val lapsed = SpacedRepetition.onFailed(learned, now)
        assertEquals(0, lapsed.box)
        assertEquals(now, lapsed.dueAtMillis)
        assertEquals(1, lapsed.failed)
        assertEquals(learned.solved, lapsed.solved)
    }

    @Test
    fun unseenPuzzlesNearThePlayerRatingComeFirst() {
        val chosen = SpacedRepetition.nextPuzzle(BundledPuzzles.all, emptyMap(), playerRating = 1400, nowMillis = now)
        val expected = BundledPuzzles.all.minByOrNull { kotlin.math.abs(it.rating - 1400) }
        assertEquals(expected?.id, assertNotNull(chosen).id)
    }

    @Test
    fun puzzlesThatAreNotDueYetAreSkipped() {
        val progress = BundledPuzzles.all
            .dropLast(1)
            .associate { it.id to PuzzleProgress(it.id, box = 3, dueAtMillis = now + 1000) }
        val chosen = SpacedRepetition.nextPuzzle(BundledPuzzles.all, progress, playerRating = 1200, nowMillis = now)
        assertEquals(BundledPuzzles.all.last().id, assertNotNull(chosen).id)
    }

    @Test
    fun anEmptyPackHasNoNextPuzzle() {
        assertNull(SpacedRepetition.nextPuzzle(emptyList(), emptyMap(), 1200, now))
    }

    @Test
    fun solvingAHarderPuzzleGainsMoreRatingThanAnEasierOne() {
        val hard = PuzzleElo.update(1200, 1600, solved = true)
        val easy = PuzzleElo.update(1200, 800, solved = true)
        assertTrue(hard > easy, "hard=$hard easy=$easy")
        assertTrue(hard > 1200 && easy > 1200)
    }

    @Test
    fun failingAnEasyPuzzleCostsMoreThanFailingAHardOne() {
        val easy = PuzzleElo.update(1200, 800, solved = false)
        val hard = PuzzleElo.update(1200, 1600, solved = false)
        assertTrue(easy < hard, "easy=$easy hard=$hard")
        assertTrue(easy < 1200 && hard < 1200)
    }

    @Test
    fun ratingsStayInsideTheSupportedRange() {
        var rating = PuzzleElo.DEFAULT_RATING
        repeat(400) { rating = PuzzleElo.update(rating, 2800, solved = true) }
        assertTrue(rating <= PuzzleElo.MAX_RATING)
        repeat(400) { rating = PuzzleElo.update(rating, 600, solved = false) }
        assertTrue(rating >= PuzzleElo.MIN_RATING)
    }
}
