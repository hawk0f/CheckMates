package dev.hawk0f.checkmates.ui.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.net.lichess.LichessStandingPlayer
import dev.hawk0f.checkmates.net.lichess.LichessTeam
import dev.hawk0f.checkmates.net.lichess.LichessTournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.hawk0f.checkmates.resources.lichess_sign_in_tournaments
import dev.hawk0f.checkmates.resources.Res
import org.jetbrains.compose.resources.getString

data class LichessArenasUiState(
    val loading: Boolean = true,
    val featured: LichessTournament? = null,
    val standing: List<LichessStandingPlayer> = emptyList(),
    val myRank: Int? = null,
    val starting: List<LichessTournament> = emptyList(),
    val teams: List<LichessTeam> = emptyList(),
    val joined: Set<String> = emptySet(),
    val message: String? = null
)

class LichessArenasViewModel : ViewModel() {

    private val api = LichessAuth.api
    private val _uiState = MutableStateFlow(LichessArenasUiState())
    val uiState: StateFlow<LichessArenasUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val list = runCatching { api.tournaments() }.getOrNull()
            val featured = list?.started?.firstOrNull()
            _uiState.value = _uiState.value.copy(
                loading = false,
                featured = featured,
                starting = list?.created?.take(6) ?: emptyList()
            )
            featured?.let { loadStanding(it.id) }
            LichessAuth.username.value?.let { name ->
                runCatching { api.teamsOf(name) }.onSuccess { teams ->
                    _uiState.value = _uiState.value.copy(teams = teams)
                }
            }
        }
    }

    fun join(id: String) {
        val token = LichessAuth.token
        if (token == null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(message = getString(Res.string.lichess_sign_in_tournaments))
            }
            return
        }
        viewModelScope.launch {
            val ok = runCatching { api.joinTournament(token, id) }.getOrDefault(false)
            _uiState.value = _uiState.value.copy(
                joined = if (ok) _uiState.value.joined + id else _uiState.value.joined,
                message = if (ok) {
                    "Joined. The game arrives through the event stream."
                } else {
                    "Lichess refused the join — it may be rated-only or already closed."
                }
            )
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun loadStanding(id: String) {
        viewModelScope.launch {
            runCatching { api.tournament(id) }.onSuccess { info ->
                val me = LichessAuth.username.value?.lowercase()
                val players = info.standing?.players ?: emptyList()
                val myIndex = players.indexOfFirst { it.name.lowercase() == me }
                val window = if (myIndex >= 0) {
                    players.subList(
                        (myIndex - 1).coerceAtLeast(0),
                        (myIndex + 2).coerceAtMost(players.size)
                    )
                } else {
                    players.take(3)
                }
                _uiState.value = _uiState.value.copy(
                    standing = window,
                    myRank = players.getOrNull(myIndex)?.rank
                )
            }
        }
    }
}
