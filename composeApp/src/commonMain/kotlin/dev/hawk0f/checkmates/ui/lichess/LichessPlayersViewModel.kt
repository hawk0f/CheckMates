package dev.hawk0f.checkmates.ui.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.net.lichess.LichessLeaderboardUser
import dev.hawk0f.checkmates.net.lichess.LichessUserRef
import dev.hawk0f.checkmates.net.lichess.stringAt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LichessPlayersUiState(
    val loading: Boolean = true,
    val online: List<LichessUserRef> = emptyList(),
    val offline: List<LichessUserRef> = emptyList(),
    val leaderboard: List<LichessLeaderboardUser> = emptyList(),
    val openChallengeUrl: String? = null,
    val message: String? = null
)

class LichessPlayersViewModel : ViewModel() {

    private val api = LichessAuth.api
    private val _uiState = MutableStateFlow(LichessPlayersUiState())
    val uiState: StateFlow<LichessPlayersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val token = LichessAuth.token
            if (token != null) {
                val ids = mutableListOf<String>()
                runCatching {
                    api.following(token).collect { raw ->
                        raw.stringAt("id")?.let { ids.add(it) }
                    }
                }
                val statuses = runCatching { api.usersStatus(ids.take(50)) }.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(
                    online = statuses.filter { it.online == true },
                    offline = statuses.filter { it.online != true }
                )
            }
            runCatching { api.leaderboard(10, "rapid") }.onSuccess { board ->
                _uiState.value = _uiState.value.copy(leaderboard = board.users)
            }
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    fun challenge(username: String) {
        val token = LichessAuth.token
        if (token == null) {
            _uiState.value = _uiState.value.copy(message = "Sign in to challenge players")
            return
        }
        viewModelScope.launch {
            runCatching {
                api.challengeUser(
                    token = token,
                    username = username,
                    clockLimitSeconds = 600,
                    incrementSeconds = 0,
                    rated = false
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    message = "Challenge sent to $username · 10+0 casual"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    message = it.message ?: "could not send the challenge"
                )
            }
        }
    }

    fun createOpenChallenge() {
        val token = LichessAuth.token
        if (token == null) {
            _uiState.value = _uiState.value.copy(message = "Sign in to create a link")
            return
        }
        viewModelScope.launch {
            runCatching { api.openChallenge(token, 300, 0, rated = false) }
                .onSuccess { created ->
                    _uiState.value = _uiState.value.copy(
                        openChallengeUrl = created.url ?: created.urlWhite
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        message = it.message ?: "could not create a link"
                    )
                }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
