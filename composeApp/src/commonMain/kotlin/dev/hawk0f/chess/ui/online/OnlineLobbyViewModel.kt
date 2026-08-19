package dev.hawk0f.chess.ui.online

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.chess.net.ApiClient
import dev.hawk0f.chess.net.ServerConfig
import dev.hawk0f.chess.net.WebSocketGameTransport
import dev.hawk0f.chess.net.configuredHttpClient
import dev.hawk0f.chess.session.ActiveGameSession
import dev.hawk0f.chess.session.AuthManager
import dev.hawk0f.chess.session.GameSessionHolder
import dev.hawk0f.chess.shared.protocol.GameMessage
import dev.hawk0f.chess.shared.protocol.ShortCode
import dev.hawk0f.chess.shared.protocol.TimeControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface LobbyStep {
    data object Idle : LobbyStep
    data object Working : LobbyStep
    data class WaitingForOpponent(val shortCode: String, val joinUrl: String) : LobbyStep
    data object GameReady : LobbyStep
    data class Failed(val message: String) : LobbyStep
}

data class OnlineLobbyUiState(
    val playerName: String = "",
    val codeInput: String = "",
    val step: LobbyStep = LobbyStep.Idle,
    val timeControl: TimeControl? = null
)

class OnlineLobbyViewModel : ViewModel() {

    private val httpClient = configuredHttpClient()
    private val api = ApiClient(httpClient)

    private val _uiState = MutableStateFlow(
        OnlineLobbyUiState(playerName = AuthManager.profile.value?.displayName.orEmpty().take(30))
    )
    val uiState: StateFlow<OnlineLobbyUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(playerName = value.take(30))
    }

    fun onTimeControlChange(value: TimeControl?) {
        _uiState.value = _uiState.value.copy(timeControl = value)
    }

    fun onCodeChange(value: String) {
        _uiState.value = _uiState.value.copy(codeInput = ShortCode.normalize(value).take(ShortCode.LENGTH))
    }

    fun createGame() {
        val state = _uiState.value
        if (state.step is LobbyStep.Working) {
            return
        }
        _uiState.value = state.copy(step = LobbyStep.Working)
        viewModelScope.launch {
            try {
                val created = api.createGame(state.playerName.ifBlank { "Host" }, state.timeControl)
                val transport = WebSocketGameTransport(
                    client = httpClient,
                    url = ServerConfig.wsGameUrl(created.gameId, created.playerToken),
                    gameId = created.gameId,
                    playerToken = created.playerToken
                )
                val session = ActiveGameSession(transport, kind = "online", myName = state.playerName.ifBlank { "Host" })
                GameSessionHolder.install(session)
                transport.start(session.scope)
                _uiState.value = _uiState.value.copy(
                    step = LobbyStep.WaitingForOpponent(created.shortCode, created.joinUrl)
                )
                combine(session.myColor, session.opponentName) { color, name -> color != null && name != null }
                    .filter { it }
                    .first()
                _uiState.value = _uiState.value.copy(step = LobbyStep.GameReady)
            } catch (e: Exception) {
                GameSessionHolder.clear()
                _uiState.value = _uiState.value.copy(step = LobbyStep.Failed(e.message ?: "connection failed"))
            }
        }
    }

    fun joinGame(rawInput: String? = null) {
        val state = _uiState.value
        if (state.step is LobbyStep.Working) {
            return
        }
        val code = rawInput?.let { ShortCode.extractFromText(it) } ?: state.codeInput
        if (!ShortCode.isValid(code)) {
            _uiState.value = state.copy(step = LobbyStep.Failed("Bad game code"))
            return
        }
        _uiState.value = state.copy(step = LobbyStep.Working)
        viewModelScope.launch {
            try {
                val info = api.gameInfo(code)
                if (!info.exists || info.gameId == null) {
                    _uiState.value = _uiState.value.copy(step = LobbyStep.Failed("Game not found or expired"))
                    return@launch
                }
                if (!info.joinable) {
                    _uiState.value = _uiState.value.copy(step = LobbyStep.Failed("Game already started"))
                    return@launch
                }
                val transport = WebSocketGameTransport(
                    client = httpClient,
                    url = ServerConfig.wsGameUrl(info.gameId!!),
                    firstMessage = GameMessage.JoinGame(code, state.playerName.ifBlank { "Guest" })
                )
                val session = ActiveGameSession(transport, kind = "online", myName = state.playerName.ifBlank { "Guest" })
                GameSessionHolder.install(session)
                transport.start(session.scope)
                combine(session.myColor, session.opponentName) { color, name -> color != null && name != null }
                    .filter { it }
                    .first()
                _uiState.value = _uiState.value.copy(step = LobbyStep.GameReady)
            } catch (e: Exception) {
                GameSessionHolder.clear()
                _uiState.value = _uiState.value.copy(step = LobbyStep.Failed(e.message ?: "connection failed"))
            }
        }
    }

    fun cancelWaiting() {
        GameSessionHolder.clear()
        _uiState.value = _uiState.value.copy(step = LobbyStep.Idle)
    }

    fun consumeGameReady() {
        _uiState.value = _uiState.value.copy(step = LobbyStep.Idle)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(step = LobbyStep.Idle)
    }

    override fun onCleared() {
        httpClient.close()
    }
}
