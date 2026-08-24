package dev.hawk0f.checkmates.shared.engine

import com.github.bhlangonijr.chesslib.Board
import kotlin.math.abs

enum class MoveQuality {
    BEST,
    GOOD,
    INACCURACY,
    MISTAKE,
    BLUNDER
}

data class MoveAnalysis(
    val ply: Int,
    val uci: String,
    val bestMove: String?,
    val scoreBefore: Int,
    val scoreAfter: Int,
    val centipawnLoss: Int,
    val quality: MoveQuality
)

data class AnalysisSummary(
    val moves: List<MoveAnalysis>,
    val whiteAverageLoss: Int,
    val blackAverageLoss: Int
) {
    fun blundersOf(white: Boolean): Int = moves
        .filter { (it.ply % 2 == 0) == white }
        .count { it.quality == MoveQuality.BLUNDER }
}

class GameAnalyzer(private val engine: ChessEngine = ChessEngine()) {

    fun analyse(
        uciHistory: List<String>,
        depth: Int = DEFAULT_DEPTH,
        nodeBudget: Int = DEFAULT_NODE_BUDGET,
        onProgress: (Int) -> Unit = {}
    ): AnalysisSummary {
        val board = Board()
        val moves = mutableListOf<MoveAnalysis>()
        for ((ply, uci) in uciHistory.withIndex()) {
            val before = engine.analyse(board.fen, depth, nodeBudget)
            val played = runCatching { board.doMove(moveOf(board, uci)) }.getOrNull()
            if (played != true) {
                break
            }
            val afterOpponentView = engine.analyse(board.fen, depth, nodeBudget)
            val scoreBefore = before.scoreCentipawns
            val scoreAfter = -afterOpponentView.scoreCentipawns
            val loss = (scoreBefore - scoreAfter).coerceAtLeast(0)
            moves += MoveAnalysis(
                ply = ply,
                uci = uci,
                bestMove = before.bestMove,
                scoreBefore = scoreBefore,
                scoreAfter = scoreAfter,
                centipawnLoss = loss,
                quality = qualityOf(loss, before.bestMove == uci)
            )
            onProgress(ply + 1)
        }
        return AnalysisSummary(
            moves = moves,
            whiteAverageLoss = averageLoss(moves, white = true),
            blackAverageLoss = averageLoss(moves, white = false)
        )
    }

    fun whitePerspective(analysis: MoveAnalysis): Int =
        if (analysis.ply % 2 == 0) analysis.scoreAfter else -analysis.scoreAfter

    private fun averageLoss(moves: List<MoveAnalysis>, white: Boolean): Int {
        val side = moves.filter { (it.ply % 2 == 0) == white }
        if (side.isEmpty()) {
            return 0
        }
        return side.sumOf { it.centipawnLoss } / side.size
    }

    private fun qualityOf(loss: Int, wasBest: Boolean): MoveQuality = when {
        wasBest || loss <= BEST_LOSS -> MoveQuality.BEST
        loss < INACCURACY_LOSS -> MoveQuality.GOOD
        loss < MISTAKE_LOSS -> MoveQuality.INACCURACY
        loss < BLUNDER_LOSS -> MoveQuality.MISTAKE
        else -> MoveQuality.BLUNDER
    }

    private fun moveOf(board: Board, uci: String) =
        board.legalMoves().first { it.toUci() == uci || it.toUci() == uci.lowercase() }

    companion object {
        const val DEFAULT_DEPTH = 4
        const val DEFAULT_NODE_BUDGET = 60_000
        const val BEST_LOSS = 15
        const val INACCURACY_LOSS = 60
        const val MISTAKE_LOSS = 140
        const val BLUNDER_LOSS = 300

        fun evaluationBarFraction(scoreCentipawns: Int): Float {
            val clamped = scoreCentipawns.coerceIn(-1000, 1000)
            val sign = if (clamped < 0) -1 else 1
            val magnitude = abs(clamped) / 1000f
            return 0.5f + sign * magnitude * 0.5f
        }
    }
}
