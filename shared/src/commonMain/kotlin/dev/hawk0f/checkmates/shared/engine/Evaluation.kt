package dev.hawk0f.checkmates.shared.engine

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import kotlin.math.abs

internal object Evaluation {

    const val MATE_SCORE = 30_000

    private val pieceValue = intArrayOf(100, 320, 330, 500, 900, 0)

    private val pawnTable = IntArray(64)
    private val knightTable = IntArray(64)
    private val bishopTable = IntArray(64)
    private val rookTable = IntArray(64)
    private val queenTable = IntArray(64)
    private val kingMidTable = IntArray(64)
    private val kingEndTable = IntArray(64)

    init {
        for (index in 0 until 64) {
            val file = index % 8
            val rank = index / 8
            val centreFile = 3.5 - abs(file - 3.5)
            val centreRank = 3.5 - abs(rank - 3.5)
            val centrality = ((centreFile + centreRank) * 3).toInt()

            pawnTable[index] = when (rank) {
                0, 7 -> 0
                else -> (rank - 1) * (rank - 1) * 2 + (if (file in 2..5) 6 else 0)
            }
            knightTable[index] = centrality * 2 - 14
            bishopTable[index] = centrality + (if (file == rank || file + rank == 7) 8 else 0) - 6
            rookTable[index] = (if (rank == 6) 18 else 0) + (if (file in 3..4) 6 else 0)
            queenTable[index] = centrality - 4
            kingMidTable[index] = when {
                rank == 0 && (file <= 2 || file >= 6) -> 26
                rank == 0 -> 6
                rank == 1 -> -8
                else -> -30 - rank * 4
            }
            kingEndTable[index] = centrality * 3 - 20
        }
    }

    fun evaluate(board: Board): Int {
        var score = 0
        var phase = 0
        for (index in 0 until 64) {
            val square = Square.squareAt(index)
            val piece = board.getPiece(square)
            if (piece == Piece.NONE) {
                continue
            }
            val white = piece.pieceSide == Side.WHITE
            val mirrored = if (white) index else mirror(index)
            val type = piece.pieceType ?: continue
            val material = pieceValue[type.ordinal]
            phase += phaseWeight(type)
            val positional = when (type) {
                PieceType.PAWN -> pawnTable[mirrored]
                PieceType.KNIGHT -> knightTable[mirrored]
                PieceType.BISHOP -> bishopTable[mirrored]
                PieceType.ROOK -> rookTable[mirrored]
                PieceType.QUEEN -> queenTable[mirrored]
                PieceType.KING -> 0
                else -> 0
            }
            score += if (white) material + positional else -(material + positional)
        }
        score += kingScore(board, Side.WHITE, phase) - kingScore(board, Side.BLACK, phase)
        score += bishopPair(board, Side.WHITE) - bishopPair(board, Side.BLACK)
        score += pawnStructure(board, Side.WHITE) - pawnStructure(board, Side.BLACK)
        return if (board.sideToMove == Side.WHITE) score else -score
    }

    private fun kingScore(board: Board, side: Side, phase: Int): Int {
        val square = board.getKingSquare(side)
        if (square == Square.NONE) {
            return 0
        }
        val index = square.ordinal
        val mirrored = if (side == Side.WHITE) index else mirror(index)
        val midWeight = phase.coerceIn(0, MAX_PHASE)
        val mid = kingMidTable[mirrored] * midWeight
        val end = kingEndTable[mirrored] * (MAX_PHASE - midWeight)
        return (mid + end) / MAX_PHASE
    }

    private fun bishopPair(board: Board, side: Side): Int {
        val bishop = if (side == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
        return if (countBits(board.getBitboard(bishop)) >= 2) 28 else 0
    }

    private fun pawnStructure(board: Board, side: Side): Int {
        val pawn = if (side == Side.WHITE) Piece.WHITE_PAWN else Piece.BLACK_PAWN
        val bitboard = board.getBitboard(pawn)
        var score = 0
        val perFile = IntArray(8)
        for (index in 0 until 64) {
            if (bitboard and (1L shl index) != 0L) {
                perFile[index % 8]++
            }
        }
        for (file in 0 until 8) {
            val count = perFile[file]
            if (count == 0) {
                continue
            }
            if (count > 1) {
                score -= (count - 1) * 14
            }
            val left = if (file > 0) perFile[file - 1] else 0
            val right = if (file < 7) perFile[file + 1] else 0
            if (left == 0 && right == 0) {
                score -= 12
            }
        }
        return score
    }

    private fun phaseWeight(type: PieceType): Int = when (type) {
        PieceType.KNIGHT, PieceType.BISHOP -> 1
        PieceType.ROOK -> 2
        PieceType.QUEEN -> 4
        else -> 0
    }

    private fun countBits(bitboard: Long): Int {
        var value = bitboard
        var count = 0
        while (value != 0L) {
            value = value and (value - 1)
            count++
        }
        return count
    }

    private fun mirror(index: Int): Int = index xor 56

    fun materialValue(type: PieceType): Int =
        if (type == PieceType.KING) MATE_SCORE else pieceValue[type.ordinal]

    const val MAX_PHASE = 24
}
