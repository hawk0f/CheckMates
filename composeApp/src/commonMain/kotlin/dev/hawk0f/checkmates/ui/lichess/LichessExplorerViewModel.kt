package dev.hawk0f.checkmates.ui.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.net.lichess.LichessExplorerPosition
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ExplorerSource {
    LICHESS,
    MASTERS,
    PLAYER
}

data class LichessExplorerUiState(
    val source: ExplorerSource = ExplorerSource.LICHESS,
    val loading: Boolean = false,
    val gameState: GameState? = null,
    val position: LichessExplorerPosition? = null,
    val moves: List<String> = emptyList(),
    val username: String? = null,
    val error: String? = null
)

class LichessExplorerViewModel : ViewModel() {

    private val api = LichessAuth.api
    private val _uiState = MutableStateFlow(
        LichessExplorerUiState(username = LichessAuth.username.value)
    )
    val uiState: StateFlow<LichessExplorerUiState> = _uiState.asStateFlow()

    private var game = ChessGame()

    init {
        _uiState.value = _uiState.value.copy(gameState = game.state())
        fetch()
    }

    fun onSourceChange(source: ExplorerSource) {
        if (source == ExplorerSource.PLAYER && _uiState.value.username == null) {
            _uiState.value = _uiState.value.copy(error = "sign in to see your own openings")
            return
        }
        _uiState.value = _uiState.value.copy(source = source)
        fetch()
    }

    fun playMove(uci: String) {
        game.applyUci(uci)
        _uiState.value = _uiState.value.copy(
            gameState = game.state(),
            moves = _uiState.value.moves + uci
        )
        fetch()
    }

    fun undo() {
        val history = _uiState.value.moves.dropLast(1)
        game = ChessGame()
        for (uci in history) {
            game.applyUci(uci)
        }
        _uiState.value = _uiState.value.copy(gameState = game.state(), moves = history)
        fetch()
    }

    fun reset() {
        game = ChessGame()
        _uiState.value = _uiState.value.copy(gameState = game.state(), moves = emptyList())
        fetch()
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun fetch() {
        val token = LichessAuth.token
        if (token == null) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                error = "The opening explorer needs a lichess token — sign in first."
            )
            return
        }
        val fen = game.fen()
        val state = _uiState.value
        _uiState.value = state.copy(loading = true)
        viewModelScope.launch {
            runCatching {
                when (state.source) {
                    ExplorerSource.LICHESS -> api.explorerLichess(
                        token = token,
                        fen = fen,
                        speeds = listOf("blitz", "rapid", "classical"),
                        ratings = listOf(1400, 1600, 1800)
                    )

                    ExplorerSource.MASTERS -> api.explorerMasters(token, fen)
                    ExplorerSource.PLAYER -> api.explorerPlayer(
                        token = token,
                        fen = fen,
                        player = state.username.orEmpty(),
                        color = "white"
                    )
                }
            }.onSuccess { position ->
                _uiState.value = _uiState.value.copy(loading = false, position = position)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = it.message ?: "explorer request failed"
                )
            }
        }
    }
}
