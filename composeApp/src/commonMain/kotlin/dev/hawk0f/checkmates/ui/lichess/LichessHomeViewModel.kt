package dev.hawk0f.checkmates.ui.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.net.lichess.LichessChallenge
import dev.hawk0f.checkmates.net.lichess.LichessOngoingGame
import dev.hawk0f.checkmates.net.lichess.LichessSessionStarter
import dev.hawk0f.checkmates.net.lichess.typeName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LichessHomeUiState(
    val username: String? = null,
    val ratings: List<String> = emptyList(),
    val streaming: Boolean = false,
    val ongoing: List<LichessOngoingGame> = emptyList(),
    val incoming: List<LichessChallenge> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val gameReady: Boolean = false
)

class LichessHomeViewModel : ViewModel() {

    private val api = LichessAuth.api
    private val _uiState = MutableStateFlow(LichessHomeUiState(username = LichessAuth.username.value))
    val uiState: StateFlow<LichessHomeUiState> = _uiState.asStateFlow()

    private var eventJob: Job? = null

    init {
        viewModelScope.launch {
            LichessAuth.username.collect { name ->
                _uiState.value = _uiState.value.copy(username = name)
                if (name != null) {
                    refresh()
                    startEventStream()
                } else {
                    eventJob?.cancel()
                    _uiState.value = LichessHomeUiState()
                }
            }
        }
    }

    fun startLogin(): String = LichessAuth.buildAuthorizeUrl()

    fun onAuthCallback(code: String?, state: String?, error: String?) {
        if (code == null) {
            _uiState.value = _uiState.value.copy(error = error ?: "authorization was cancelled")
            return
        }
        _uiState.value = _uiState.value.copy(loading = true)
        viewModelScope.launch {
            LichessAuth.completeLogin(code, state)
                .onSuccess { _uiState.value = _uiState.value.copy(loading = false, username = it) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = it.message ?: "sign in failed"
                    )
                }
        }
    }

    fun refresh() {
        val token = LichessAuth.token ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val games = runCatching { api.ongoingGames(token) }.getOrNull()
            val challenges = runCatching { api.challenges(token) }.getOrNull()
            val account = runCatching { api.account(token) }.getOrNull()
            _uiState.value = _uiState.value.copy(
                loading = false,
                ongoing = games ?: _uiState.value.ongoing,
                incoming = challenges?.incoming ?: _uiState.value.incoming,
                ratings = account?.let { fresh ->
                    listOf("rapid", "blitz", "classical").mapNotNull { key ->
                        fresh.perfs[key]?.rating?.let { rating -> "$rating $key" }
                    }
                } ?: _uiState.value.ratings
            )
        }
    }

    fun openGame(gameId: String) {
        if (LichessSessionStarter.open(gameId)) {
            _uiState.value = _uiState.value.copy(gameReady = true)
        } else {
            _uiState.value = _uiState.value.copy(error = "sign in first")
        }
    }

    fun acceptChallenge(id: String) {
        val token = LichessAuth.token ?: return
        viewModelScope.launch {
            runCatching { api.acceptChallenge(token, id) }
            _uiState.value = _uiState.value.copy(
                incoming = _uiState.value.incoming.filterNot { it.id == id }
            )
        }
    }

    fun declineChallenge(id: String) {
        val token = LichessAuth.token ?: return
        _uiState.value = _uiState.value.copy(
            incoming = _uiState.value.incoming.filterNot { it.id == id }
        )
        viewModelScope.launch {
            runCatching { api.declineChallenge(token, id) }
        }
    }

    fun consumeGameReady() {
        _uiState.value = _uiState.value.copy(gameReady = false)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun logout() {
        eventJob?.cancel()
        LichessAuth.logout()
        _uiState.value = LichessHomeUiState()
    }

    private fun startEventStream() {
        val token = LichessAuth.token ?: return
        if (eventJob?.isActive == true) {
            return
        }
        eventJob = viewModelScope.launch {
            while (LichessAuth.token != null) {
                runCatching {
                    api.eventStream(token).collect { event ->
                        _uiState.value = _uiState.value.copy(streaming = true)
                        when (event.typeName()) {
                            "gameStart", "gameFinish", "challenge", "challengeDeclined",
                            "challengeCanceled" -> refresh()

                            else -> {}
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(streaming = false)
                if (LichessAuth.token == null) {
                    break
                }
                delay(3000)
            }
        }
    }
}
