package dev.hawk0f.checkmates.shared.domain

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece as LibPiece
import com.github.bhlangonijr.chesslib.PieceType as LibPieceType
import com.github.bhlangonijr.chesslib.Side as LibSide
import com.github.bhlangonijr.chesslib.Square as LibSquare
import com.github.bhlangonijr.chesslib.move.Move as LibMove

sealed interface MoveOutcome {
    data class Applied(val state: GameState) : MoveOutcome
    data object Illegal : MoveOutcome
}

class ChessGame {

    private val board = Board()
    private val history = mutableListOf<String>()
    private var agreedResult: GameResult? = null

    fun state(): GameState {
        val pieces = buildMap {
            for (index in 0..63) {
                val libSquare = LibSquare.squareAt(index)
                val libPiece = board.getPiece(libSquare)
                if (libPiece != LibPiece.NONE) {
                    put(Square(index), libPiece.toDomain())
                }
            }
        }
        return GameState(
            fen = board.getFen(includeCounters = true),
            pieces = pieces,
            sideToMove = board.sideToMove.toDomain(),
            lastMove = history.lastOrNull()?.let { Square.fromUci(it.substring(0, 2)) to Square.fromUci(it.substring(2, 4)) },
            inCheck = board.isKingAttacked,
            result = agreedResult ?: detectEnding(),
            uciHistory = history.toList()
        )
    }

    fun fen(): String = board.getFen(includeCounters = true)

    fun sideToMove(): PieceColor = board.sideToMove.toDomain()

    fun pieceAt(square: Square): Piece? =
        board.getPiece(LibSquare.squareAt(square.index)).takeIf { it != LibPiece.NONE }?.toDomain()

    fun legalDestinations(from: Square): Set<Square> {
        val libFrom = LibSquare.squareAt(from.index)
        return board.legalMoves()
            .filter { it.from == libFrom }
            .map { Square(it.to.ordinal) }
            .toSet()
    }

    fun isPromotionMove(from: Square, to: Square): Boolean {
        val libFrom = LibSquare.squareAt(from.index)
        val libTo = LibSquare.squareAt(to.index)
        return board.legalMoves().any { it.from == libFrom && it.to == libTo && it.promotion != LibPiece.NONE }
    }

    fun applyUci(uci: String): MoveOutcome {
        if (agreedResult != null || detectEnding() != null) {
            return MoveOutcome.Illegal
        }
        val move = runCatching { LibMove(uci, board.sideToMove) }.getOrNull() ?: return MoveOutcome.Illegal
        if (move !in board.legalMoves()) {
            return MoveOutcome.Illegal
        }
        board.doMove(move)
        history.add(uci)
        return MoveOutcome.Applied(state())
    }

    fun finish(reason: GameOverReason, winner: PieceColor?) {
        agreedResult = GameResult(reason, winner)
    }

    fun loadFen(fen: String) {
        board.loadFromFen(fen)
        history.clear()
        agreedResult = null
    }

    private fun detectEnding(): GameResult? = when {
        board.isMated -> GameResult(GameOverReason.CHECKMATE, board.sideToMove.toDomain().opposite)
        board.isStaleMate -> GameResult(GameOverReason.STALEMATE, null)
        board.isInsufficientMaterial -> GameResult(GameOverReason.INSUFFICIENT_MATERIAL, null)
        board.isRepetition(3) -> GameResult(GameOverReason.REPETITION, null)
        board.halfMoveCounter >= 100 -> GameResult(GameOverReason.FIFTY_MOVE, null)
        else -> null
    }
}

private fun LibSide.toDomain(): PieceColor = if (this == LibSide.WHITE) PieceColor.WHITE else PieceColor.BLACK

private fun LibPiece.toDomain(): Piece = Piece(
    color = pieceSide!!.toDomain(),
    kind = when (pieceType) {
        LibPieceType.PAWN -> PieceKind.PAWN
        LibPieceType.KNIGHT -> PieceKind.KNIGHT
        LibPieceType.BISHOP -> PieceKind.BISHOP
        LibPieceType.ROOK -> PieceKind.ROOK
        LibPieceType.QUEEN -> PieceKind.QUEEN
        LibPieceType.KING -> PieceKind.KING
        else -> error("Unexpected piece type: $pieceType")
    }
)
