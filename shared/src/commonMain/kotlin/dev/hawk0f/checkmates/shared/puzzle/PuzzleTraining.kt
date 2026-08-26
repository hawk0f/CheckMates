package dev.hawk0f.checkmates.shared.puzzle

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

@Serializable
data class PuzzleProgress(
    val puzzleId: String,
    val box: Int = 0,
    val dueAtMillis: Long = 0,
    val solved: Int = 0,
    val failed: Int = 0
)

object SpacedRepetition {

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    val intervalsMillis = longArrayOf(
        0,
        DAY_MILLIS,
        3 * DAY_MILLIS,
        7 * DAY_MILLIS,
        16 * DAY_MILLIS,
        35 * DAY_MILLIS
    )

    val maxBox: Int get() = intervalsMillis.lastIndex

    fun onSolved(progress: PuzzleProgress, nowMillis: Long): PuzzleProgress {
        val box = (progress.box + 1).coerceAtMost(maxBox)
        return progress.copy(
            box = box,
            dueAtMillis = nowMillis + intervalsMillis[box],
            solved = progress.solved + 1
        )
    }

    fun onFailed(progress: PuzzleProgress, nowMillis: Long): PuzzleProgress = progress.copy(
        box = 0,
        dueAtMillis = nowMillis + intervalsMillis[0],
        failed = progress.failed + 1
    )

    fun nextPuzzle(
        puzzles: List<Puzzle>,
        progress: Map<String, PuzzleProgress>,
        playerRating: Int,
        nowMillis: Long,
        currentId: String? = null
    ): Puzzle? {
        val candidates = puzzles.filterNot { it.id == currentId }.ifEmpty { puzzles }
        val due = candidates.filter { puzzle ->
            val entry = progress[puzzle.id] ?: return@filter true
            entry.dueAtMillis <= nowMillis
        }
        val pool = due.ifEmpty { candidates }
        return pool.minByOrNull { puzzle ->
            val entry = progress[puzzle.id]
            val attempts = (entry?.solved ?: 0) + (entry?.failed ?: 0)
            val seenPenalty = attempts * 400
            abs(puzzle.rating - playerRating) + seenPenalty
        }
    }
}

object PuzzleElo {

    const val DEFAULT_RATING = 1200
    const val K_FACTOR = 32
    const val MIN_RATING = 600
    const val MAX_RATING = 2800

    fun update(playerRating: Int, puzzleRating: Int, solved: Boolean): Int {
        val expected = 1.0 / (1.0 + 10.0.pow((puzzleRating - playerRating) / 400.0))
        val score = if (solved) 1.0 else 0.0
        val updated = playerRating + K_FACTOR * (score - expected)
        return updated.roundToInt().coerceIn(MIN_RATING, MAX_RATING)
    }
}
