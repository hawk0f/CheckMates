package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.engine.ChessEngine
import dev.hawk0f.checkmates.shared.engine.EngineLevel
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EngineTest {

    private val engine = ChessEngine(Random(7))

    @Test
    fun findsMateInOne() {
        val fen = "6k1/5ppp/8/8/8/8/8/R3K2R w KQ - 0 1"
        assertEquals("a1a8", engine.bestMove(fen, EngineLevel.FIVE))
    }

    @Test
    fun findsMateInTwo() {
        val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 4 4"
        assertEquals("f3f7", engine.bestMove(fen, EngineLevel.FIVE))
    }

    @Test
    fun takesTheFreeQueen() {
        val fen = "rnb1kbnr/pppp1ppp/8/4p3/6q1/5P2/PPPPP1PP/RNBQKBNR w KQkq - 0 3"
        val move = engine.bestMove(fen, EngineLevel.FIVE)
        assertEquals("f3g4", move)
    }

    @Test
    fun avoidsHangingItsOwnQueen() {
        val fen = "rnb1kbnr/pppp1ppp/8/4p3/6q1/8/PPPPPPPP/RNBQKBNR b KQkq - 0 3"
        val move = engine.bestMove(fen, EngineLevel.FIVE)
        assertNotNull(move)
        assertTrue(move != "g4g2", "g4g2 hangs the queen to the king")
    }

    @Test
    fun everyLevelReturnsALegalMove() {
        val fen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
        for (level in EngineLevel.entries) {
            val move = engine.bestMove(fen, level)
            assertNotNull(move, "level ${level.id} returned nothing")
            val game = ChessGame()
            game.loadFen(fen)
            assertTrue(game.applyUci(move) is MoveOutcome.Applied, "level ${level.id} played illegal $move")
        }
    }

    @Test
    fun reportsAnEvaluationForAWinningPosition() {
        val line = engine.analyse("8/8/8/8/8/5k2/6q1/7K b - - 0 1", depth = 4)
        assertNotNull(line.bestMove)
        assertTrue(line.scoreCentipawns > 500, "black is winning here: ${line.scoreCentipawns}")
    }

    @Test
    fun promotesToAQueen() {
        val move = engine.bestMove("8/P6k/8/8/8/8/7K/8 w - - 0 1", EngineLevel.FIVE)
        assertEquals("a7a8q", move)
    }
}
