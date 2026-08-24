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

    fun analyse(uciHistory: List<String>) {
        if (_uiState.value.running || uciHistory.isEmpty()) {
            return
        }
        _uiState.value = ReplayAnalysisUiState(running = true, totalPlies = uciHistory.size)
        job = viewModelScope.launch {
            try {
                val summary = withContext(analysisContext) {
                    analyzer.analyse(uciHistory) { done ->
                        _uiState.value = _uiState.value.copy(analysedPlies = done)
                    }
                }
                _uiState.value = _uiState.value.copy(running = false, summary = summary)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.value = ReplayAnalysisUiState()
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _uiState.value = ReplayAnalysisUiState()
    }
}
