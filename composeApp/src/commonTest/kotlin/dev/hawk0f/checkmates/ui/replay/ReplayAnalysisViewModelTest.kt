package dev.hawk0f.checkmates.ui.replay

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.session.PuzzlePersistence
import dev.hawk0f.checkmates.session.ReplayAnnotationPersistence
import dev.hawk0f.checkmates.shared.puzzle.Puzzle
import dev.hawk0f.checkmates.shared.puzzle.PuzzleProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ReplayAnalysisViewModelTest {

    private class FakeAnnotationStore : ReplayAnnotationPersistence {
        private val values = mutableMapOf<Pair<Long, Int>, String>()

        override fun annotations(gameId: Long): Map<Int, String> = values
            .filterKeys { it.first == gameId }
            .mapKeys { it.key.second }

        override fun save(gameId: Long, ply: Int, text: String) {
            val key = gameId to ply
            if (text.trim().isEmpty()) {
                values.remove(key)
            } else {
                values[key] = text.trim()
            }
        }
    }

    private class FakePuzzleStore : PuzzlePersistence {
        val personal = mutableListOf<Puzzle>()

        override fun loadProgress(): Map<String, PuzzleProgress> = emptyMap()
        override fun saveProgress(progress: Map<String, PuzzleProgress>) = Unit
        override fun loadRating(): Int = 1200
        override fun saveRating(rating: Int) = Unit
        override fun loadStreak(): Int = 0
        override fun saveStreak(streak: Int) = Unit
        override fun loadPersonalPuzzles(): List<Puzzle> = personal
        override fun savePersonalPuzzle(puzzle: Puzzle) {
            personal.removeAll { it.id == puzzle.id }
            personal += puzzle
        }
    }

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun analysisProducesASummaryForEveryPly() = runTest(dispatcher) {
        val viewModel = ReplayAnalysisViewModel(analysisContext = dispatcher)
        val moves = listOf("e2e4", "e7e5", "g1f3", "b8c6")

        viewModel.analyse(moves)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.running)
        val summary = assertNotNull(state.summary)
        assertEquals(moves.size, summary.moves.size)
        assertEquals(moves, summary.moves.map { it.uci })
    }

    @Test
    fun anEmptyGameIsNeverAnalysed() = runTest(dispatcher) {
        val viewModel = ReplayAnalysisViewModel(analysisContext = dispatcher)
        viewModel.analyse(emptyList())
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.running)
        assertNull(viewModel.uiState.value.summary)
    }

    @Test
    fun progressIsReportedWhileAnalysisRuns() = runTest(dispatcher) {
        val viewModel = ReplayAnalysisViewModel(analysisContext = dispatcher)
        viewModel.analyse(listOf("e2e4", "e7e5"))
        dispatcher.scheduler.runCurrent()

        assertEquals(2, viewModel.uiState.value.totalPlies)

        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.analysedPlies)
    }

    @Test
    fun cancellingClearsTheState() = runTest(dispatcher) {
        val viewModel = ReplayAnalysisViewModel(analysisContext = dispatcher)
        viewModel.analyse(listOf("e2e4", "e7e5"))
        dispatcher.scheduler.runCurrent()
        viewModel.cancel()

        assertFalse(viewModel.uiState.value.running)
        assertNull(viewModel.uiState.value.summary)
    }

    @Test
    fun practiceStartsBeforeTheReviewedMoveAndAcceptsTheBestMove() = runTest(dispatcher) {
        val viewModel = ReplayAnalysisViewModel(analysisContext = dispatcher)
        val history = listOf("e2e4", "e7e5", "g1f3")

        viewModel.startPractice(history, ply = 2, bestMove = "g1f3")

        val initial = assertNotNull(viewModel.practiceState.value.gameState)
        assertEquals(listOf("e2e4", "e7e5"), initial.uciHistory)
        viewModel.onPracticeSquareTap(Square.fromUci("g1"))
        viewModel.onPracticeSquareTap(Square.fromUci("f3"))

        val solved = viewModel.practiceState.value
        assertEquals(PracticeResult.CORRECT, solved.result)
        assertEquals(history, solved.gameState?.uciHistory)
    }

    @Test
    fun practiceRejectsAnotherLegalMoveWithoutChangingThePosition() = runTest(dispatcher) {
        val viewModel = ReplayAnalysisViewModel(analysisContext = dispatcher)

        viewModel.startPractice(listOf("e2e4"), ply = 0, bestMove = "e2e4")
        viewModel.onPracticeSquareTap(Square.fromUci("d2"))
        viewModel.onPracticeSquareTap(Square.fromUci("d4"))

        val state = viewModel.practiceState.value
        assertEquals(PracticeResult.TRY_AGAIN, state.result)
        assertEquals(emptyList(), state.gameState?.uciHistory)
    }

    @Test
    fun anAnalysedMistakeCanBeSavedAsAPersonalPuzzle() = runTest(dispatcher) {
        val store = FakePuzzleStore()
        val viewModel = ReplayAnalysisViewModel(analysisContext = dispatcher, puzzleStore = store)
        val history = listOf("e2e4", "e7e5", "g1f3")

        viewModel.savePersonalPuzzle(
            gameId = 42,
            uciHistory = history,
            ply = 2,
            bestMove = "f1c4",
            centipawnLoss = 320
        )

        val puzzle = store.personal.single()
        assertEquals("personal-42-2", puzzle.id)
        assertEquals(listOf("f1c4"), puzzle.solution)
        assertEquals(listOf(2), viewModel.uiState.value.savedPuzzlePlies.toList())
    }

    @Test
    fun annotationsAreStoredPerGameAndPly() = runTest(dispatcher) {
        val store = FakeAnnotationStore()
        val viewModel = ReplayAnalysisViewModel(analysisContext = dispatcher, annotationStore = store)

        viewModel.saveAnnotation(gameId = 7, ply = 3, text = "  Attack the pinned knight  ")
        viewModel.saveAnnotation(gameId = 8, ply = 3, text = "Another game")
        viewModel.loadAnnotations(7)

        assertEquals(mapOf(3 to "Attack the pinned knight"), viewModel.uiState.value.annotations)
    }

    @Test
    fun anEmptyAnnotationDeletesTheExistingNote() = runTest(dispatcher) {
        val store = FakeAnnotationStore()
        val viewModel = ReplayAnalysisViewModel(analysisContext = dispatcher, annotationStore = store)

        viewModel.saveAnnotation(7, 1, "Candidate move")
        viewModel.saveAnnotation(7, 1, "   ")

        assertEquals(emptyMap(), viewModel.uiState.value.annotations)
    }
}
