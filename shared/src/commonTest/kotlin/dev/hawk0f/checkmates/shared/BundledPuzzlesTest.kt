package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.engine.ChessEngine
import dev.hawk0f.checkmates.shared.puzzle.BundledPuzzles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BundledPuzzlesTest {

    private val engine = ChessEngine()

    @Test
    fun everyPuzzleHasAUniqueId() {
        val ids = BundledPuzzles.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(BundledPuzzles.all.isNotEmpty())
    }

    @Test
    fun everySolutionMoveIsLegalInItsPosition() {
        for (puzzle in BundledPuzzles.all) {
            val game = ChessGame()
            game.loadFen(puzzle.fen)
            for (uci in puzzle.solution) {
                val outcome = game.applyUci(uci)
                assertTrue(outcome is MoveOutcome.Applied, "${puzzle.id}: move $uci was rejected")
            }
        }
    }

    @Test
    fun theEngineAgreesWithEverySolution() {
        for (puzzle in BundledPuzzles.all) {
            val line = engine.analyse(puzzle.fen, depth = 4, nodeBudget = 200_000)
            assertEquals(puzzle.solution.first(), line.bestMove, "${puzzle.id} disagrees")
        }
    }
}
