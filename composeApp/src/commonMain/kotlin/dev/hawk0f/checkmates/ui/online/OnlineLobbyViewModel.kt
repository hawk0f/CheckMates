package dev.hawk0f.checkmates.ui.online

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.ApiClient
import dev.hawk0f.checkmates.net.SeekClient
import dev.hawk0f.checkmates.net.ServerConfig
import dev.hawk0f.checkmates.net.WebSocketGameTransport
import dev.hawk0f.checkmates.net.configuredHttpClient
import dev.hawk0f.checkmates.net.configuredWebSocketClient
import dev.hawk0f.checkmates.session.ActiveGameSession
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.lobby_bad_code
import dev.hawk0f.checkmates.resources.lobby_connection_failed
import dev.hawk0f.checkmates.resources.lobby_game_already_started
import dev.hawk0f.checkmates.resources.lobby_game_not_found
import dev.hawk0f.checkmates.resources.lobby_matchmaking_failed
import org.jetbrains.compose.resources.getString
import dev.hawk0f.checkmates.session.GameSessionHolder
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.protocol.SeekMessage
import dev.hawk0f.checkmates.shared.protocol.ShortCode
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface LobbyStep {
    data object Idle : LobbyStep
    data object Working : LobbyStep
    data class WaitingForOpponent(val shortCode: String, val joinUrl: String) : LobbyStep
    data class Searching(val queued: Int, val rating: Int) : LobbyStep
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
    private val socketClient = configuredWebSocketClient()
    private val api = ApiClient(httpClient)
    private val seekClient = SeekClient(socketClient)
    private var seekJob: Job? = null

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
                val created = api.createGame(
                    hostName = state.playerName.ifBlank { "Host" },
                    timeControl = state.timeControl,
                    authToken = AuthManager.token
                )
                val transport = WebSocketGameTransport(
                    client = socketClient,
                    url = ServerConfig.wsGameUrl(created.gameId, created.playerToken),
                    gameId = created.gameId,
                    playerToken = created.playerToken
                )
                val session = ActiveGameSession(
                    transport = transport,
                    kind = "online",
                    myName = state.playerName.ifBlank { "Host" },
                    gameId = created.gameId,
                    playerToken = created.playerToken
                )
                GameSessionHolder.install(session)
                transport.start(session.scope)
                _uiState.value = _uiState.value.copy(
                    step = LobbyStep.WaitingForOpponent(created.shortCode, created.joinUrl)
                )
                combine(session.myColor, session.opponentName) { color, name -> color != null && name != null }
                    .filter { it }
                    .first()
                _uiState.value = _uiState.value.copy(step = LobbyStep.GameReady)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                GameSessionHolder.detach()
                _uiState.value = _uiState.value.copy(step = LobbyStep.Failed(getString(Res.string.lobby_connection_failed)))
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
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    step = LobbyStep.Failed(getString(Res.string.lobby_bad_code))
                )
            }
            return
        }
        _uiState.value = state.copy(step = LobbyStep.Working)
        viewModelScope.launch {
            try {
                val info = api.gameInfo(code)
                if (!info.exists || info.gameId == null) {
                    _uiState.value = _uiState.value.copy(step = LobbyStep.Failed(getString(Res.string.lobby_game_not_found)))
                    return@launch
                }
                if (!info.joinable) {
                    _uiState.value = _uiState.value.copy(step = LobbyStep.Failed(getString(Res.string.lobby_game_already_started)))
                    return@launch
                }
                val transport = WebSocketGameTransport(
                    client = socketClient,
                    url = ServerConfig.wsGameUrl(info.gameId!!),
                    firstMessage = GameMessage.JoinGame(
                        code = code,
                        playerName = state.playerName.ifBlank { "Guest" },
                        authToken = AuthManager.token
                    )
                )
                val session = ActiveGameSession(transport, kind = "online", myName = state.playerName.ifBlank { "Guest" })
                GameSessionHolder.install(session)
                transport.start(session.scope)
                combine(session.myColor, session.opponentName) { color, name -> color != null && name != null }
                    .filter { it }
                    .first()
                _uiState.value = _uiState.value.copy(step = LobbyStep.GameReady)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                GameSessionHolder.detach()
                _uiState.value = _uiState.value.copy(step = LobbyStep.Failed(getString(Res.string.lobby_connection_failed)))
            }
        }
    }

    fun quickPair() {
        val state = _uiState.value
        if (state.step is LobbyStep.Working || state.step is LobbyStep.Searching) {
            return
        }
        val timeControl = state.timeControl ?: DEFAULT_SEEK_TIME_CONTROL
        val name = state.playerName.ifBlank { "Player" }
        _uiState.value = state.copy(step = LobbyStep.Searching(queued = 0, rating = 0))
        seekJob = viewModelScope.launch {
            try {
                seekClient.seek(name, timeControl, AuthManager.token).collect { message ->
                    when (message) {
                        is SeekMessage.Waiting -> {
                            _uiState.value = _uiState.value.copy(
                                step = LobbyStep.Searching(message.queued, message.rating)
                            )
                        }

                        is SeekMessage.Matched -> startPairedGame(message, name)
                        is SeekMessage.Error -> {
                            _uiState.value = _uiState.value.copy(step = LobbyStep.Failed(message.message))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                GameSessionHolder.detach()
                _uiState.value = _uiState.value.copy(step = LobbyStep.Failed(getString(Res.string.lobby_matchmaking_failed)))
            }
        }
    }

    private suspend fun startPairedGame(matched: SeekMessage.Matched, name: String) {
        val transport = WebSocketGameTransport(
            client = socketClient,
            url = ServerConfig.wsGameUrl(matched.gameId, matched.playerToken),
            gameId = matched.gameId,
            playerToken = matched.playerToken
        )
        val session = ActiveGameSession(
            transport = transport,
            kind = "online",
            myName = name,
            gameId = matched.gameId,
            playerToken = matched.playerToken
        )
        GameSessionHolder.install(session)
        transport.start(session.scope)
        session.myColor.filter { it != null }.first()
        _uiState.value = _uiState.value.copy(step = LobbyStep.GameReady)
    }

    fun cancelSearch() {
        seekJob?.cancel()
        seekJob = null
        GameSessionHolder.clear()
        _uiState.value = _uiState.value.copy(step = LobbyStep.Idle)
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
        seekJob?.cancel()
        httpClient.close()
        socketClient.close()
    }

    companion object {
        val DEFAULT_SEEK_TIME_CONTROL = TimeControl(180, 2)
    }
}
