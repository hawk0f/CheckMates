package dev.hawk0f.checkmates.ui.replay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.shared.engine.AnalysisSummary
import dev.hawk0f.checkmates.shared.engine.GameAnalyzer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReplayAnalysisUiState(
    val running: Boolean = false,
    val analysedPlies: Int = 0,
    val totalPlies: Int = 0,
    val summary: AnalysisSummary? = null
)

class ReplayAnalysisViewModel(
    private val analyzer: GameAnalyzer = GameAnalyzer(),
    private val analysisContext: CoroutineContext = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReplayAnalysisUiState())
    val uiState: StateFlow<ReplayAnalysisUiState> = _uiState.asStateFlow()

    private var job: Job? = null
    private var generation = 0

    fun analyse(uciHistory: List<String>) {
        if (_uiState.value.running || uciHistory.isEmpty()) {
            return
        }
        generation++
        val current = generation
        _uiState.value = ReplayAnalysisUiState(running = true, totalPlies = uciHistory.size)
        job = viewModelScope.launch {
            try {
                val summary = withContext(analysisContext) {
                    analyzer.analyse(
                        uciHistory = uciHistory,
                        shouldContinue = { current == generation }
                    ) { done ->
                        if (current == generation) {
                            _uiState.value = _uiState.value.copy(analysedPlies = done)
                        }
                    }
                }
                if (current == generation) {
                    _uiState.value = _uiState.value.copy(running = false, summary = summary)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (current == generation) {
                    _uiState.value = ReplayAnalysisUiState()
                }
            }
        }
    }

    fun cancel() {
        generation++
        job?.cancel()
        job = null
        _uiState.value = ReplayAnalysisUiState()
    }

    override fun onCleared() {
        cancel()
    }
}
