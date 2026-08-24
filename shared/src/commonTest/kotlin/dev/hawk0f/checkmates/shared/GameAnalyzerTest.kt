package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.engine.GameAnalyzer
import dev.hawk0f.checkmates.shared.engine.MoveQuality
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameAnalyzerTest {

    private val analyzer = GameAnalyzer()

    @Test
    fun hangingTheQueenIsFlaggedAsABlunder() {
        val moves = listOf("e2e4", "e7e5", "d1h5", "b8c6", "h5f7")
        val summary = analyzer.analyse(moves, depth = 3, nodeBudget = 40_000)
        assertEquals(moves.size, summary.moves.size)
        val queenGrab = summary.moves.last()
        assertEquals("h5f7", queenGrab.uci)
        assertEquals(MoveQuality.BLUNDER, queenGrab.quality)
        assertTrue(queenGrab.centipawnLoss > GameAnalyzer.BLUNDER_LOSS, "loss was ${queenGrab.centipawnLoss}")
    }

    @Test
    fun sensibleOpeningMovesAreNotFlagged() {
        val summary = analyzer.analyse(listOf("e2e4", "e7e5", "g1f3", "b8c6"), depth = 3, nodeBudget = 40_000)
        assertTrue(summary.moves.none { it.quality == MoveQuality.BLUNDER })
        assertTrue(summary.whiteAverageLoss < GameAnalyzer.MISTAKE_LOSS, "white lost ${summary.whiteAverageLoss}")
        assertTrue(summary.blackAverageLoss < GameAnalyzer.MISTAKE_LOSS, "black lost ${summary.blackAverageLoss}")
    }

    @Test
    fun analysisReportsProgressForEveryPly() {
        val seen = mutableListOf<Int>()
        analyzer.analyse(listOf("e2e4", "e7e5"), depth = 2, nodeBudget = 20_000) { seen += it }
        assertEquals(listOf(1, 2), seen)
    }

    @Test
    fun illegalHistoryStopsAtTheLastLegalMove() {
        val summary = analyzer.analyse(listOf("e2e4", "e7e5", "e4e5"), depth = 2, nodeBudget = 20_000)
        assertEquals(2, summary.moves.size)
    }

    @Test
    fun evaluationBarStaysInsideTheUnitRange() {
        assertEquals(0.5f, GameAnalyzer.evaluationBarFraction(0))
        assertTrue(GameAnalyzer.evaluationBarFraction(5000) in 0f..1f)
        assertTrue(GameAnalyzer.evaluationBarFraction(-5000) in 0f..1f)
        assertTrue(GameAnalyzer.evaluationBarFraction(300) > 0.5f)
        assertTrue(GameAnalyzer.evaluationBarFraction(-300) < 0.5f)
    }

    @Test
    fun blunderCountIsAttributedToTheRightSide() {
        val summary = analyzer.analyse(
            listOf("e2e4", "e7e5", "g1f3", "d8h4", "f3h4"),
            depth = 3,
            nodeBudget = 40_000
        )
        assertTrue(summary.blundersOf(white = false) >= 1)
        assertEquals(0, summary.blundersOf(white = true))
    }
}
