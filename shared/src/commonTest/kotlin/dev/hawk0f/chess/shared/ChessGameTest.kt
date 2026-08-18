package dev.hawk0f.chess.shared

import dev.hawk0f.chess.shared.domain.ChessGame
import dev.hawk0f.chess.shared.domain.GameOverReason
import dev.hawk0f.chess.shared.domain.MoveOutcome
import dev.hawk0f.chess.shared.domain.Piece
import dev.hawk0f.chess.shared.domain.PieceColor
import dev.hawk0f.chess.shared.domain.PieceKind
import dev.hawk0f.chess.shared.domain.Square
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChessGameTest {

    private fun ChessGame.play(vararg moves: String) {
        for (move in moves) {
            assertIs<MoveOutcome.Applied>(applyUci(move), "expected $move to be legal")
        }
    }

    @Test
    fun scholarsMateEndsInCheckmate() {
        val game = ChessGame()
        game.play("e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7")
        val result = game.state().result
        assertEquals(GameOverReason.CHECKMATE, result?.reason)
        assertEquals(PieceColor.WHITE, result?.winner)
    }

    @Test
    fun kingsideCastlingMovesRook() {
        val game = ChessGame()
        game.play("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "e1g1")
        assertEquals(Piece(PieceColor.WHITE, PieceKind.KING), game.pieceAt(Square.fromUci("g1")))
        assertEquals(Piece(PieceColor.WHITE, PieceKind.ROOK), game.pieceAt(Square.fromUci("f1")))
        assertNull(game.pieceAt(Square.fromUci("h1")))
    }

    @Test
    fun queensideCastlingMovesRook() {
        val game = ChessGame()
        game.play("d2d4", "d7d5", "b1c3", "b8c6", "c1f4", "c8f5", "d1d2", "d8d7", "e1c1")
        assertEquals(Piece(PieceColor.WHITE, PieceKind.KING), game.pieceAt(Square.fromUci("c1")))
        assertEquals(Piece(PieceColor.WHITE, PieceKind.ROOK), game.pieceAt(Square.fromUci("d1")))
        assertNull(game.pieceAt(Square.fromUci("a1")))
    }

    @Test
    fun enPassantCapturesPawn() {
        val game = ChessGame()
        game.play("e2e4", "a7a6", "e4e5", "d7d5", "e5d6")
        assertNull(game.pieceAt(Square.fromUci("d5")))
        assertEquals(Piece(PieceColor.WHITE, PieceKind.PAWN), game.pieceAt(Square.fromUci("d6")))
    }

    @Test
    fun promotionProducesChosenPiece() {
        for ((suffix, kind) in mapOf("q" to PieceKind.QUEEN, "r" to PieceKind.ROOK, "b" to PieceKind.BISHOP, "n" to PieceKind.KNIGHT)) {
            val game = ChessGame()
            game.loadFen("8/P6k/8/8/8/8/6K1/8 w - - 0 1")
            assertTrue(game.isPromotionMove(Square.fromUci("a7"), Square.fromUci("a8")))
            game.play("a7a8$suffix")
            assertEquals(Piece(PieceColor.WHITE, kind), game.pieceAt(Square.fromUci("a8")))
        }
    }

    @Test
    fun stalematePositionIsDetected() {
        val game = ChessGame()
        game.loadFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
        assertEquals(GameOverReason.STALEMATE, game.state().result?.reason)
        assertIs<MoveOutcome.Illegal>(game.applyUci("h8h7"))
    }

    @Test
    fun threefoldRepetitionIsDetected() {
        val game = ChessGame()
        game.play("g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8")
        assertEquals(GameOverReason.REPETITION, game.state().result?.reason)
    }

    @Test
    fun illegalMovesAreRejected() {
        val game = ChessGame()
        assertIs<MoveOutcome.Illegal>(game.applyUci("e2e5"))
        assertIs<MoveOutcome.Illegal>(game.applyUci("e7e5"))
        assertIs<MoveOutcome.Illegal>(game.applyUci("junk"))
        assertIs<MoveOutcome.Applied>(game.applyUci("e2e4"))
    }

    @Test
    fun movesAfterGameOverAreRejected() {
        val game = ChessGame()
        game.play("e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7")
        assertIs<MoveOutcome.Illegal>(game.applyUci("c6d4"))
    }

    @Test
    fun fenRoundTripPreservesPosition() {
        val game = ChessGame()
        game.play("e2e4", "c7c5", "g1f3")
        val fen = game.fen()
        val restored = ChessGame()
        restored.loadFen(fen)
        assertEquals(fen, restored.fen())
        assertEquals(game.state().pieces, restored.state().pieces)
        assertEquals(PieceColor.BLACK, restored.sideToMove())
    }

    @Test
    fun legalDestinationsMatchOpeningKnight() {
        val game = ChessGame()
        assertEquals(
            setOf(Square.fromUci("a3"), Square.fromUci("c3")),
            game.legalDestinations(Square.fromUci("b1"))
        )
    }

    @Test
    fun checkIsReflectedInState() {
        val game = ChessGame()
        game.play("e2e4", "f7f6", "d1h5")
        assertTrue(game.state().inCheck)
        assertNull(game.state().result)
    }

    @Test
    fun resignationFinishesGame() {
        val game = ChessGame()
        game.play("e2e4")
        game.finish(GameOverReason.RESIGNATION, PieceColor.BLACK)
        assertEquals(GameOverReason.RESIGNATION, game.state().result?.reason)
        assertEquals(PieceColor.BLACK, game.state().result?.winner)
        assertIs<MoveOutcome.Illegal>(game.applyUci("e7e5"))
    }

    @Test
    fun squareUciRoundTrip() {
        for (index in 0..63) {
            val square = Square(index)
            assertEquals(square, Square.fromUci(square.toUci()))
        }
        assertEquals("e2", Square.fromUci("e2").toUci())
        assertEquals(4, Square.fromUci("e2").file)
        assertEquals(1, Square.fromUci("e2").rank)
    }
}
