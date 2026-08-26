package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.engine.ChessEngine
import dev.hawk0f.checkmates.shared.engine.GameAnalyzer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineTerminalTest {

    @Test
    fun aStalematedPositionIsScoredAsADraw() {
        val engine = ChessEngine()
        val line = engine.analyse("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1", depth = 2, nodeBudget = 20_000)

        assertNull(line.bestMove)
        assertEquals(0, line.scoreCentipawns)
    }

    @Test
    fun aSearchStopsWhenTheCallerCancels() {
        val engine = ChessEngine()
        val line = engine.analyse(
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            depth = 6,
            nodeBudget = 1_000_000,
            shouldContinue = { false }
        )

        assertTrue(line.depth <= 1, "search ran to depth ${line.depth}")
    }

    @Test
    fun aMateBlunderDoesNotInflateAverageLoss() {
        val analyzer = GameAnalyzer()
        val summary = analyzer.analyse(
            uciHistory = listOf("f2f3", "e7e5", "g2g4", "d8h4"),
            depth = 2,
            nodeBudget = 20_000
        )

        assertTrue(summary.whiteAverageLoss <= GameAnalyzer.MAX_LOSS_EVAL, "loss ${summary.whiteAverageLoss}")
    }
}
