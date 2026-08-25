package dev.hawk0f.checkmates.ui.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.net.lichess.LichessLeaderboardUser
import dev.hawk0f.checkmates.net.lichess.LichessUserRef
import dev.hawk0f.checkmates.net.lichess.stringAt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.hawk0f.checkmates.resources.lichess_challenge_send_failed
import dev.hawk0f.checkmates.resources.lichess_challenge_sent_casual
import dev.hawk0f.checkmates.resources.lichess_sign_in_challenge
import dev.hawk0f.checkmates.resources.lichess_sign_in_link
import dev.hawk0f.checkmates.resources.Res
import org.jetbrains.compose.resources.getString

data class LichessPlayersUiState(
    val loading: Boolean = true,
    val query: String = "",
    val suggestions: List<String> = emptyList(),
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

    private var suggestJob: Job? = null

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        val query = value.trim().take(30)
        _uiState.value = _uiState.value.copy(query = value.take(30))
        suggestJob?.cancel()
        if (query.length < 3) {
            _uiState.value = _uiState.value.copy(suggestions = emptyList())
            return
        }
        suggestJob = viewModelScope.launch {
            delay(300)
            runCatching { api.autocompletePlayers(query) }
                .onSuccess { names -> _uiState.value = _uiState.value.copy(suggestions = names.take(6)) }
                .onFailure { _uiState.value = _uiState.value.copy(suggestions = emptyList()) }
        }
    }

    fun clearQuery() {
        suggestJob?.cancel()
        _uiState.value = _uiState.value.copy(query = "", suggestions = emptyList())
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
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(message = getString(Res.string.lichess_sign_in_challenge))
            }
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
                    message = getString(Res.string.lichess_challenge_sent_casual, username)
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    message = it.message ?: getString(Res.string.lichess_challenge_send_failed)
                )
            }
        }
    }

    fun createOpenChallenge() {
        val token = LichessAuth.token
        if (token == null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(message = getString(Res.string.lichess_sign_in_link))
            }
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
