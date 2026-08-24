package dev.hawk0f.checkmates.ui.replay

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ReplayAnalysisViewModelTest {

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
}
