package dev.hawk0f.checkmates.ui.openings

import dev.hawk0f.checkmates.session.OpeningProgressPersistence
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.shared.opening.OpeningBook
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private class FakeOpeningProgress : OpeningProgressPersistence {
    val saved = mutableListOf<Triple<String, Int, Int>>()
    override fun bestStreak(lineId: String): Int = 0
    override fun mistakes(lineId: String): Int = 0
    override fun saveResult(lineId: String, mistakes: Int, streak: Int) {
        saved += Triple(lineId, mistakes, streak)
    }
}

class OpeningDrillViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun play(viewModel: OpeningDrillViewModel, uci: String) {
        viewModel.onSquareTap(Square.fromUci(uci.take(2)))
        viewModel.onSquareTap(Square.fromUci(uci.drop(2).take(2)))
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun playingTheWholeLineCompletesTheDrill() = runTest(dispatcher) {
        val store = FakeOpeningProgress()
        val line = OpeningBook.byId("italian")!!
        val viewModel = OpeningDrillViewModel("italian", store, opponentDelayMillis = 0)
        dispatcher.scheduler.advanceUntilIdle()

        for ((index, uci) in line.moves.withIndex()) {
            if (index % 2 != 0) {
                continue
            }
            play(viewModel, uci)
        }

        assertEquals(DrillStatus.COMPLETED, viewModel.uiState.value.status)
        assertEquals(0, viewModel.uiState.value.mistakes)
        assertEquals(listOf(Triple("italian", 0, 1)), store.saved)
    }

    @Test
    fun aWrongMoveIsRejectedAndTheBookMoveIsShown() = runTest(dispatcher) {
        val viewModel = OpeningDrillViewModel("italian", FakeOpeningProgress(), opponentDelayMillis = 0)
        dispatcher.scheduler.advanceUntilIdle()

        play(viewModel, "d2d4")

        val state = viewModel.uiState.value
        assertEquals(DrillStatus.WRONG_MOVE, state.status)
        assertEquals("e2e4", state.expectedMove)
        assertEquals(1, state.mistakes)
        assertTrue(state.gameState.uciHistory.isEmpty())
    }

    @Test
    fun theOpponentPlaysItsBookReplyAutomatically() = runTest(dispatcher) {
        val viewModel = OpeningDrillViewModel("italian", FakeOpeningProgress(), opponentDelayMillis = 0)
        dispatcher.scheduler.advanceUntilIdle()

        play(viewModel, "e2e4")

        assertEquals(listOf("e2e4", "e7e5"), viewModel.uiState.value.gameState.uciHistory)
    }

    @Test
    fun blackLinesStartWithTheOpponentMove() = runTest(dispatcher) {
        val viewModel = OpeningDrillViewModel("french", FakeOpeningProgress(), opponentDelayMillis = 0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("e2e4"), viewModel.uiState.value.gameState.uciHistory)
    }

    @Test
    fun restartingClearsMistakesAndReplaysTheOpening() = runTest(dispatcher) {
        val viewModel = OpeningDrillViewModel("french", FakeOpeningProgress(), opponentDelayMillis = 0)
        dispatcher.scheduler.advanceUntilIdle()
        play(viewModel, "e7e5")
        assertEquals(DrillStatus.WRONG_MOVE, viewModel.uiState.value.status)

        viewModel.restart()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DrillStatus.PLAYING, state.status)
        assertEquals(0, state.mistakes)
        assertEquals(listOf("e2e4"), state.gameState.uciHistory)
    }
}
