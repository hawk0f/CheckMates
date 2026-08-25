package dev.hawk0f.checkmates.ui.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.net.lichess.LichessCloudEval
import dev.hawk0f.checkmates.net.lichess.LichessGameExport
import dev.hawk0f.checkmates.net.lichess.LichessSessionStarter
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.hawk0f.checkmates.resources.lichess_review_running
import dev.hawk0f.checkmates.resources.lichess_review_unfinished
import dev.hawk0f.checkmates.resources.Res
import org.jetbrains.compose.resources.getString

data class LichessReviewUiState(
    val gameId: String,
    val loading: Boolean = true,
    val blocked: String? = null,
    val export: LichessGameExport? = null,
    val moves: List<String> = emptyList(),
    val moveIndex: Int = 0,
    val gameState: GameState? = null,
    val eval: LichessCloudEval? = null,
    val evalMissing: Boolean = false,
    val error: String? = null
)

class LichessReviewViewModel(gameId: String) : ViewModel() {

    private val api = LichessAuth.api
    private val _uiState = MutableStateFlow(LichessReviewUiState(gameId = gameId))
    val uiState: StateFlow<LichessReviewUiState> = _uiState.asStateFlow()

    private var game = ChessGame()
    private var evalJob: Job? = null

    init {
        load()
    }

    fun goTo(index: Int) {
        val moves = _uiState.value.moves
        val bounded = index.coerceIn(0, moves.size)
        game = ChessGame()
        for (uci in moves.take(bounded)) {
            game.applyUci(uci)
        }
        _uiState.value = _uiState.value.copy(moveIndex = bounded, gameState = game.state())
        requestEval()
    }

    fun step(delta: Int) {
        goTo(_uiState.value.moveIndex + delta)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun load() {
        val gameId = _uiState.value.gameId
        if (LichessSessionStarter.currentGameId() == gameId) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    blocked = getString(Res.string.lichess_review_running)
                )
            }
            return
        }
        viewModelScope.launch {
            runCatching { api.gameExport(gameId) }
                .onSuccess { export ->
                    if (export.status == "started" || export.status == "created") {
                        _uiState.value = _uiState.value.copy(
                            loading = false,
                            blocked = getString(Res.string.lichess_review_unfinished)
                        )
                        return@onSuccess
                    }
                    val moves = export.moves.split(' ').filter { it.isNotEmpty() }
                    game = ChessGame()
                    for (uci in moves) {
                        game.applyUci(uci)
                    }
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        export = export,
                        moves = moves,
                        moveIndex = moves.size,
                        gameState = game.state()
                    )
                    requestEval()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = it.message ?: "could not load the game"
                    )
                }
        }
    }

    private fun requestEval() {
        if (_uiState.value.blocked != null) {
            return
        }
        val fen = game.fen()
        evalJob?.cancel()
        evalJob = viewModelScope.launch {
            runCatching { api.cloudEval(fen, 3) }
                .onSuccess { _uiState.value = _uiState.value.copy(eval = it, evalMissing = false) }
                .onFailure { _uiState.value = _uiState.value.copy(eval = null, evalMissing = true) }
        }
    }
}
