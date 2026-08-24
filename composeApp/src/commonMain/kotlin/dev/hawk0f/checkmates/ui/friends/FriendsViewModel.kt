package dev.hawk0f.checkmates.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.ApiClient
import dev.hawk0f.checkmates.net.ServerConfig
import dev.hawk0f.checkmates.net.WebSocketGameTransport
import dev.hawk0f.checkmates.net.configuredHttpClient
import dev.hawk0f.checkmates.net.configuredWebSocketClient
import dev.hawk0f.checkmates.session.ActiveGameSession
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.session.GameSessionHolder
import dev.hawk0f.checkmates.shared.protocol.FriendSummary
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendsUiState(
    val friends: List<FriendSummary> = emptyList(),
    val recentOpponents: List<FriendSummary> = emptyList(),
    val nameInput: String = "",
    val loading: Boolean = true,
    val working: Boolean = false,
    val error: String? = null,
    val challengeCode: String? = null,
    val gameReady: Boolean = false
)

class FriendsViewModel(
    private val timeControl: TimeControl = TimeControl(300, 3)
) : ViewModel() {

    private val httpClient = configuredHttpClient()
    private val socketClient = configuredWebSocketClient()
    private val api = ApiClient(httpClient)

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(nameInput = value.take(40))
    }

    fun refresh() {
        val token = AuthManager.token
        if (token == null) {
            _uiState.value = _uiState.value.copy(loading = false)
            return
        }
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val response = api.friends(token)
                _uiState.value = _uiState.value.copy(
                    friends = response.friends,
                    recentOpponents = response.recentOpponents,
                    loading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "could not load")
            }
        }
    }

    fun addFriend() {
        val token = AuthManager.token ?: return
        val name = _uiState.value.nameInput.trim()
        if (name.isEmpty() || _uiState.value.working) {
            return
        }
        _uiState.value = _uiState.value.copy(working = true, error = null)
        viewModelScope.launch {
            try {
                api.addFriend(token, name)
                _uiState.value = _uiState.value.copy(nameInput = "", working = false)
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(working = false, error = e.message ?: "could not add")
            }
        }
    }

    fun removeFriend(friend: FriendSummary) {
        val token = AuthManager.token ?: return
        viewModelScope.launch {
            runCatching { api.removeFriend(token, friend.userId) }
            refresh()
        }
    }

    fun challenge(friend: FriendSummary) {
        val token = AuthManager.token ?: return
        if (_uiState.value.working || friend.userId < 0) {
            return
        }
        _uiState.value = _uiState.value.copy(working = true, error = null)
        viewModelScope.launch {
            try {
                val challenge = api.challenge(token, friend.userId, timeControl)
                val transport = WebSocketGameTransport(
                    client = socketClient,
                    url = ServerConfig.wsGameUrl(challenge.gameId, challenge.playerToken),
                    gameId = challenge.gameId,
                    playerToken = challenge.playerToken
                )
                val myName = AuthManager.profile.value?.displayName.orEmpty().ifBlank { "Host" }
                val session = ActiveGameSession(transport, kind = "online", myName = myName)
                GameSessionHolder.install(session)
                transport.start(session.scope)
                _uiState.value = _uiState.value.copy(
                    working = false,
                    challengeCode = challenge.shortCode,
                    gameReady = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                GameSessionHolder.clear()
                _uiState.value = _uiState.value.copy(working = false, error = e.message ?: "challenge failed")
            }
        }
    }

    fun consumeGameReady() {
        _uiState.value = _uiState.value.copy(gameReady = false)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        httpClient.close()
    }
}
