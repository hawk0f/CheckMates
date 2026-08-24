package dev.hawk0f.checkmates.shared.domain

sealed interface PositionValidity {
    data class Valid(val fen: String) : PositionValidity
    data class Invalid(val reason: PositionProblem) : PositionValidity
}

enum class PositionProblem {
    MISSING_WHITE_KING,
    MISSING_BLACK_KING,
    TOO_MANY_KINGS,
    PAWN_ON_LAST_RANK,
    OPPONENT_IN_CHECK,
    NOT_A_POSITION
}

object PositionEditor {

    fun buildFen(
        pieces: Map<Square, Piece>,
        sideToMove: PieceColor,
        castling: String = "-",
        enPassant: String = "-"
    ): String {
        val placement = (7 downTo 0).joinToString("/") { rank ->
            buildString {
                var empty = 0
                for (file in 0..7) {
                    val piece = pieces[Square.of(file, rank)]
                    if (piece == null) {
                        empty++
                        continue
                    }
                    if (empty > 0) {
                        append(empty)
                        empty = 0
                    }
                    append(piece.symbol())
                }
                if (empty > 0) {
                    append(empty)
                }
            }
        }
        val side = if (sideToMove == PieceColor.WHITE) "w" else "b"
        return "$placement $side ${castling.ifBlank { "-" }} ${enPassant.ifBlank { "-" }} 0 1"
    }

    fun validate(pieces: Map<Square, Piece>, sideToMove: PieceColor): PositionValidity {
        val kings = pieces.values.filter { it.kind == PieceKind.KING }
        val whiteKings = kings.count { it.color == PieceColor.WHITE }
        val blackKings = kings.count { it.color == PieceColor.BLACK }
        if (whiteKings == 0) {
            return PositionValidity.Invalid(PositionProblem.MISSING_WHITE_KING)
        }
        if (blackKings == 0) {
            return PositionValidity.Invalid(PositionProblem.MISSING_BLACK_KING)
        }
        if (whiteKings > 1 || blackKings > 1) {
            return PositionValidity.Invalid(PositionProblem.TOO_MANY_KINGS)
        }
        val pawnOnEdge = pieces.any { (square, piece) ->
            piece.kind == PieceKind.PAWN && (square.rank == 0 || square.rank == 7)
        }
        if (pawnOnEdge) {
            return PositionValidity.Invalid(PositionProblem.PAWN_ON_LAST_RANK)
        }

        val fen = buildFen(pieces, sideToMove)
        val game = ChessGame()
        val loaded = runCatching { game.loadFen(fen) }.isSuccess
        if (!loaded) {
            return PositionValidity.Invalid(PositionProblem.NOT_A_POSITION)
        }
        val mirrored = ChessGame()
        val mirroredFen = buildFen(pieces, sideToMove.opposite)
        if (runCatching { mirrored.loadFen(mirroredFen) }.isSuccess && mirrored.state().inCheck) {
            return PositionValidity.Invalid(PositionProblem.OPPONENT_IN_CHECK)
        }
        return PositionValidity.Valid(fen)
    }

    fun piecesFromFen(fen: String): Map<Square, Piece>? {
        val game = ChessGame()
        return runCatching {
            game.loadFen(fen)
            game.state().pieces
        }.getOrNull()
    }

    fun sideToMoveFromFen(fen: String): PieceColor =
        if (fen.split(" ").getOrNull(1) == "b") PieceColor.BLACK else PieceColor.WHITE

    private fun Piece.symbol(): Char {
        val letter = when (kind) {
            PieceKind.PAWN -> 'p'
            PieceKind.KNIGHT -> 'n'
            PieceKind.BISHOP -> 'b'
            PieceKind.ROOK -> 'r'
            PieceKind.QUEEN -> 'q'
            PieceKind.KING -> 'k'
        }
        return if (color == PieceColor.WHITE) letter.uppercaseChar() else letter
    }
}
