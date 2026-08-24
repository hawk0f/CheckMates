package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.opening.OpeningBook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpeningBookTest {

    @Test
    fun everyLineIsLegalFromTheStartingPosition() {
        for (line in OpeningBook.lines) {
            val game = ChessGame()
            for (uci in line.moves) {
                assertTrue(
                    game.applyUci(uci) is MoveOutcome.Applied,
                    "${line.id}: move $uci was rejected"
                )
            }
        }
    }

    @Test
    fun everyLineHasAUniqueIdAndAName() {
        val ids = OpeningBook.lines.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(OpeningBook.lines.all { it.name.isNotBlank() })
    }

    @Test
    fun theTrainedSideActuallyHasMovesInTheLine() {
        for (line in OpeningBook.lines) {
            val trainedPlies = line.moves.indices.filter { index ->
                (index % 2 == 0) == (line.trainedColor == PieceColor.WHITE)
            }
            assertTrue(trainedPlies.isNotEmpty(), "${line.id} has nothing to train")
        }
    }

    @Test
    fun linesAreOfferedForBothColours() {
        assertTrue(OpeningBook.forColor(PieceColor.WHITE).isNotEmpty())
        assertTrue(OpeningBook.forColor(PieceColor.BLACK).isNotEmpty())
        assertEquals(OpeningBook.lines.size, OpeningBook.forColor(PieceColor.WHITE).size + OpeningBook.forColor(PieceColor.BLACK).size)
    }

    @Test
    fun linesCanBeLookedUpById() {
        assertNotNull(OpeningBook.byId("italian"))
        assertEquals(null, OpeningBook.byId("does-not-exist"))
    }
}
