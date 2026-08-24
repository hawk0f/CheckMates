package dev.hawk0f.checkmates.shared.engine

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.move.Move
import kotlin.random.Random

enum class EngineLevel(val id: Int, val depth: Int, val blunderChance: Double, val nodeBudget: Int) {
    ONE(1, 1, 0.30, 20_000),
    TWO(2, 2, 0.18, 40_000),
    THREE(3, 3, 0.10, 80_000),
    FOUR(4, 4, 0.05, 150_000),
    FIVE(5, 5, 0.0, 250_000),
    SIX(6, 6, 0.0, 400_000),
    SEVEN(7, 7, 0.0, 600_000),
    EIGHT(8, 8, 0.0, 900_000);

    companion object {
        val DEFAULT = THREE

        fun byId(id: Int): EngineLevel = entries.find { it.id == id } ?: DEFAULT
    }
}

data class EngineLine(
    val bestMove: String?,
    val scoreCentipawns: Int,
    val mateInPlies: Int?,
    val depth: Int
)

class ChessEngine(private val random: Random = Random.Default) {

    fun bestMove(fen: String, level: EngineLevel = EngineLevel.DEFAULT): String? {
        val board = loadBoard(fen) ?: return null
        val moves = board.legalMoves()
        if (moves.isEmpty()) {
            return null
        }
        if (level.blunderChance > 0 && random.nextDouble() < level.blunderChance) {
            return moves[random.nextInt(moves.size)].toUci()
        }
        return analyse(board, level.depth, level.nodeBudget).bestMove ?: moves.first().toUci()
    }

    fun analyse(fen: String, depth: Int = 6, nodeBudget: Int = 400_000): EngineLine {
        val board = loadBoard(fen) ?: return EngineLine(null, 0, null, 0)
        return analyse(board, depth, nodeBudget)
    }

    private fun analyse(board: Board, depth: Int, nodeBudget: Int): EngineLine {
        val search = Search(nodeBudget)
        var best: Move? = null
        var bestScore = 0
        var reached = 0
        for (currentDepth in 1..depth) {
            val result = search.root(board, currentDepth, best)
            if (search.aborted && currentDepth > 1) {
                break
            }
            best = result.move
            bestScore = result.score
            reached = currentDepth
            if (isMateScore(bestScore)) {
                break
            }
        }
        return EngineLine(
            bestMove = best?.toUci(),
            scoreCentipawns = bestScore,
            mateInPlies = mateDistance(bestScore),
            depth = reached
        )
    }

    private fun loadBoard(fen: String): Board? = runCatching {
        Board().apply { loadFromFen(fen) }
    }.getOrNull()

    private fun isMateScore(score: Int): Boolean =
        score > Evaluation.MATE_SCORE - MAX_PLY || score < -Evaluation.MATE_SCORE + MAX_PLY

    private fun mateDistance(score: Int): Int? = when {
        score > Evaluation.MATE_SCORE - MAX_PLY -> Evaluation.MATE_SCORE - score
        score < -Evaluation.MATE_SCORE + MAX_PLY -> -(Evaluation.MATE_SCORE + score)
        else -> null
    }

    private class RootResult(val move: Move?, val score: Int)

    private class Search(private val nodeBudget: Int) {

        var aborted = false
            private set

        private var nodes = 0
        private val killers = arrayOfNulls<Move>(MAX_PLY * 2)

        fun root(board: Board, depth: Int, previousBest: Move?): RootResult {
            nodes = 0
            aborted = false
            var alpha = -Evaluation.MATE_SCORE
            val beta = Evaluation.MATE_SCORE
            var bestMove: Move? = null
            var bestScore = -Evaluation.MATE_SCORE
            for (move in ordered(board, 0, previousBest)) {
                board.doMove(move)
                val score = -negamax(board, depth - 1, 1, -beta, -alpha)
                board.undoMove()
                if (aborted && bestMove != null) {
                    break
                }
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
                if (score > alpha) {
                    alpha = score
                }
            }
            return RootResult(bestMove, bestScore)
        }

        private fun negamax(board: Board, depth: Int, ply: Int, alphaIn: Int, beta: Int): Int {
            if (aborted) {
                return 0
            }
            nodes++
            if (nodes > nodeBudget) {
                aborted = true
                return 0
            }
            if (board.isMated) {
                return -Evaluation.MATE_SCORE + ply
            }
            if (board.isDraw || board.isStaleMate) {
                return 0
            }
            if (depth <= 0 || ply >= MAX_PLY) {
                return quiescence(board, ply, alphaIn, beta)
            }
            var alpha = alphaIn
            var best = -Evaluation.MATE_SCORE
            for (move in ordered(board, ply, null)) {
                board.doMove(move)
                val score = -negamax(board, depth - 1, ply + 1, -beta, -alpha)
                board.undoMove()
                if (score > best) {
                    best = score
                }
                if (score > alpha) {
                    alpha = score
                }
                if (alpha >= beta) {
                    if (!isCapture(board, move)) {
                        killers[ply] = move
                    }
                    break
                }
            }
            return best
        }

        private fun quiescence(board: Board, ply: Int, alphaIn: Int, beta: Int): Int {
            nodes++
            if (nodes > nodeBudget) {
                aborted = true
                return 0
            }
            if (board.isMated) {
                return -Evaluation.MATE_SCORE + ply
            }
            if (board.isDraw || board.isStaleMate) {
                return 0
            }
            val standPat = Evaluation.evaluate(board)
            if (standPat >= beta || ply >= MAX_PLY) {
                return standPat
            }
            var alpha = if (standPat > alphaIn) standPat else alphaIn
            for (move in board.legalMoves()) {
                if (!isCapture(board, move) && move.promotion == Piece.NONE) {
                    continue
                }
                board.doMove(move)
                val score = -quiescence(board, ply + 1, -beta, -alpha)
                board.undoMove()
                if (score > alpha) {
                    alpha = score
                }
                if (alpha >= beta) {
                    break
                }
            }
            return alpha
        }

        private fun ordered(board: Board, ply: Int, first: Move?): List<Move> {
            val killer = killers.getOrNull(ply)
            return board.legalMoves().sortedByDescending { move ->
                when {
                    move == first -> 1_000_000
                    move.promotion != Piece.NONE -> 900_000
                    isCapture(board, move) -> 500_000 + captureGain(board, move)
                    move == killer -> 400_000
                    else -> 0
                }
            }
        }

        private fun captureGain(board: Board, move: Move): Int {
            val victim = board.getPiece(move.to)
            val attacker = board.getPiece(move.from)
            val victimType = victim.pieceType ?: PieceType.PAWN
            val victimValue = Evaluation.materialValue(victimType)
            val attackerType = attacker.pieceType
            val attackerValue = if (attackerType == null) 0 else Evaluation.materialValue(attackerType)
            return victimValue * 10 - attackerValue
        }

        private fun isCapture(board: Board, move: Move): Boolean {
            if (board.getPiece(move.to) != Piece.NONE) {
                return true
            }
            val moving = board.getPiece(move.from)
            return moving != Piece.NONE &&
                moving.pieceType == PieceType.PAWN &&
                move.from.file != move.to.file
        }
    }

    companion object {
        const val MAX_PLY = 64
    }
}

internal fun Move.toUci(): String {
    val promotion = when (promotion.pieceType) {
        PieceType.QUEEN -> "q"
        PieceType.ROOK -> "r"
        PieceType.BISHOP -> "b"
        PieceType.KNIGHT -> "n"
        else -> ""
    }
    return "${from.value().lowercase()}${to.value().lowercase()}$promotion"
}
