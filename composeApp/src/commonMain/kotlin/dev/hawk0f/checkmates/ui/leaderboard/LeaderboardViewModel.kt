package dev.hawk0f.checkmates.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.ApiClient
import dev.hawk0f.checkmates.net.configuredHttpClient
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.shared.protocol.GameSpeed
import dev.hawk0f.checkmates.shared.protocol.LeaderboardEntry
import dev.hawk0f.checkmates.shared.protocol.RatingEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val speed: GameSpeed = GameSpeed.BLITZ,
    val entries: List<LeaderboardEntry> = emptyList(),
    val myRatings: List<RatingEntry> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

class LeaderboardViewModel : ViewModel() {

    private val httpClient = configuredHttpClient()
    private val api = ApiClient(httpClient)

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        load(GameSpeed.BLITZ)
        loadMyRatings()
    }

    fun selectSpeed(speed: GameSpeed) {
        if (speed == _uiState.value.speed && _uiState.value.entries.isNotEmpty()) {
            return
        }
        load(speed)
    }

    private fun load(speed: GameSpeed) {
        _uiState.value = _uiState.value.copy(speed = speed, loading = true, error = null)
        viewModelScope.launch {
            try {
                val response = api.leaderboard(speed)
                _uiState.value = _uiState.value.copy(
                    speed = response.speed,
                    entries = response.entries,
                    loading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "could not load")
            }
        }
    }

    private fun loadMyRatings() {
        val token = AuthManager.token ?: return
        viewModelScope.launch {
            val ratings = runCatching { api.ratings(token).ratings }.getOrNull() ?: return@launch
            _uiState.value = _uiState.value.copy(myRatings = ratings)
        }
    }

    override fun onCleared() {
        httpClient.close()
    }
}
