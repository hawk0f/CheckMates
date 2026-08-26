package dev.hawk0f.checkmates.ui.puzzle

import dev.hawk0f.checkmates.session.PuzzlePersistence
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.shared.puzzle.Puzzle
import dev.hawk0f.checkmates.shared.puzzle.PuzzleElo
import dev.hawk0f.checkmates.shared.puzzle.PuzzleProgress
import dev.hawk0f.checkmates.shared.puzzle.PuzzleTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakePuzzleStore : PuzzlePersistence {
    var progress: Map<String, PuzzleProgress> = emptyMap()
    var rating: Int = PuzzleElo.DEFAULT_RATING
    var streak: Int = 0

    override fun loadProgress(): Map<String, PuzzleProgress> = progress
    override fun saveProgress(progress: Map<String, PuzzleProgress>) {
        this.progress = progress
    }

    override fun loadRating(): Int = rating
    override fun saveRating(rating: Int) {
        this.rating = rating
    }

    override fun loadStreak(): Int = streak
    override fun saveStreak(streak: Int) {
        this.streak = streak
    }
}

class PuzzleViewModelTest {

    private val twoMover = Puzzle(
        id = "two-mover",
        fen = "6k1/5p1p/6p1/8/8/8/5PPP/R5K1 w - - 0 1",
        solution = listOf("a1a8", "g8g7", "a8a7"),
        rating = 1200,
        theme = PuzzleTheme.MATE_IN_TWO
    )

    private fun newViewModel(store: FakePuzzleStore) = PuzzleViewModel(
        store = store,
        puzzles = listOf(twoMover),
        now = { 1_700_000_000_000 }
    )

    private fun PuzzleViewModel.play(uci: String) {
        onSquareTap(Square.fromUci(uci.substring(0, 2))!!)
        onSquareTap(Square.fromUci(uci.substring(2, 4))!!)
    }

    @Test
    fun theOpponentReplyIsPlayedAndTheSecondMoveFinishesThePuzzle() {
        val store = FakePuzzleStore()
        val viewModel = newViewModel(store)

        viewModel.play("a1a8")
        val afterFirst = viewModel.uiState.value
        assertEquals(PuzzleOutcome.UNSOLVED, afterFirst.outcome)
        assertEquals(listOf("a1a8", "g8g7"), afterFirst.gameState?.uciHistory)

        viewModel.play("a8a7")
        val solved = viewModel.uiState.value
        assertEquals(PuzzleOutcome.SOLVED, solved.outcome)
        assertEquals(1, solved.streak)
        assertTrue(solved.ratingDelta > 0)
    }

    @Test
    fun aWrongSecondMoveFailsThePuzzle() {
        val store = FakePuzzleStore()
        val viewModel = newViewModel(store)

        viewModel.play("a1a8")
        viewModel.play("a8a1")

        assertEquals(PuzzleOutcome.FAILED, viewModel.uiState.value.outcome)
        assertEquals(0, store.streak)
    }

    @Test
    fun promotionWaitsForTheChosenPiece() {
        val underpromotion = Puzzle(
            id = "underpromotion",
            fen = "8/P6k/8/8/8/8/6p1/6K1 w - - 0 1",
            solution = listOf("a7a8n"),
            rating = 1200,
            theme = PuzzleTheme.PROMOTION
        )
        val viewModel = PuzzleViewModel(
            store = FakePuzzleStore(),
            puzzles = listOf(underpromotion),
            now = { 1_700_000_000_000 }
        )

        viewModel.play("a7a8")
        val pending = viewModel.uiState.value
        assertEquals(Square.fromUci("a7") to Square.fromUci("a8"), pending.pendingPromotion)
        assertEquals(PuzzleOutcome.UNSOLVED, pending.outcome)

        viewModel.onPromotionChosen(PieceKind.KNIGHT)
        val solved = viewModel.uiState.value
        assertEquals(null, solved.pendingPromotion)
        assertEquals(PuzzleOutcome.SOLVED, solved.outcome)
    }

    @Test
    fun theHintPointsAtTheMoveThatIsDueNext() {
        val store = FakePuzzleStore()
        val viewModel = newViewModel(store)

        viewModel.showHint()
        assertEquals(Square.fromUci("a1"), viewModel.uiState.value.hintSquare)

        viewModel.play("a1a8")
        viewModel.showHint()
        assertEquals(Square.fromUci("a8"), viewModel.uiState.value.hintSquare)
    }
}
