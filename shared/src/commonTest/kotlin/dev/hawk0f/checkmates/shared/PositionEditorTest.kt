package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.Piece
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.domain.PositionEditor
import dev.hawk0f.checkmates.shared.domain.PositionProblem
import dev.hawk0f.checkmates.shared.domain.PositionValidity
import dev.hawk0f.checkmates.shared.domain.Square
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PositionEditorTest {

    private val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    private fun pieces(vararg entries: Pair<String, Piece>): Map<Square, Piece> =
        entries.associate { (square, piece) -> Square.fromUci(square) to piece }

    private fun king(color: PieceColor) = Piece(color, PieceKind.KING)

    @Test
    fun theStartingPositionSurvivesARoundTrip() {
        val parsed = assertNotNull(PositionEditor.piecesFromFen(startFen))
        val rebuilt = PositionEditor.buildFen(parsed, PieceColor.WHITE, castling = "KQkq")
        assertEquals(startFen, rebuilt)
    }

    @Test
    fun sideToMoveIsReadFromTheFen() {
        assertEquals(PieceColor.WHITE, PositionEditor.sideToMoveFromFen(startFen))
        assertEquals(
            PieceColor.BLACK,
            PositionEditor.sideToMoveFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1")
        )
    }

    @Test
    fun aPositionWithBothKingsIsValidAndPlayable() {
        val board = pieces(
            "e1" to king(PieceColor.WHITE),
            "e8" to king(PieceColor.BLACK),
            "d1" to Piece(PieceColor.WHITE, PieceKind.QUEEN)
        )
        val validity = PositionEditor.validate(board, PieceColor.WHITE)
        val fen = (validity as PositionValidity.Valid).fen
        val game = ChessGame()
        game.loadFen(fen)
        assertTrue(game.state().pieces.size == 3)
    }

    @Test
    fun missingKingsAreReported() {
        val onlyWhite = pieces("e1" to king(PieceColor.WHITE))
        assertEquals(
            PositionProblem.MISSING_BLACK_KING,
            (PositionEditor.validate(onlyWhite, PieceColor.WHITE) as PositionValidity.Invalid).reason
        )
        val onlyBlack = pieces("e8" to king(PieceColor.BLACK))
        assertEquals(
            PositionProblem.MISSING_WHITE_KING,
            (PositionEditor.validate(onlyBlack, PieceColor.WHITE) as PositionValidity.Invalid).reason
        )
    }

    @Test
    fun twoKingsOfTheSameColourAreRejected() {
        val board = pieces(
            "e1" to king(PieceColor.WHITE),
            "a1" to king(PieceColor.WHITE),
            "e8" to king(PieceColor.BLACK)
        )
        assertEquals(
            PositionProblem.TOO_MANY_KINGS,
            (PositionEditor.validate(board, PieceColor.WHITE) as PositionValidity.Invalid).reason
        )
    }

    @Test
    fun pawnsOnTheFirstOrLastRankAreRejected() {
        val board = pieces(
            "e1" to king(PieceColor.WHITE),
            "e8" to king(PieceColor.BLACK),
            "a8" to Piece(PieceColor.WHITE, PieceKind.PAWN)
        )
        assertEquals(
            PositionProblem.PAWN_ON_LAST_RANK,
            (PositionEditor.validate(board, PieceColor.WHITE) as PositionValidity.Invalid).reason
        )
    }

    @Test
    fun leavingTheSideNotToMoveInCheckIsRejected() {
        val board = pieces(
            "e1" to king(PieceColor.WHITE),
            "e8" to king(PieceColor.BLACK),
            "e7" to Piece(PieceColor.WHITE, PieceKind.ROOK)
        )
        assertEquals(
            PositionProblem.OPPONENT_IN_CHECK,
            (PositionEditor.validate(board, PieceColor.WHITE) as PositionValidity.Invalid).reason
        )
        assertTrue(PositionEditor.validate(board, PieceColor.BLACK) is PositionValidity.Valid)
    }

    @Test
    fun emptySquaresAreCollapsedIntoCounts() {
        val board = pieces("e1" to king(PieceColor.WHITE), "e8" to king(PieceColor.BLACK))
        assertEquals("4k3/8/8/8/8/8/8/4K3 w - - 0 1", PositionEditor.buildFen(board, PieceColor.WHITE))
    }
}
