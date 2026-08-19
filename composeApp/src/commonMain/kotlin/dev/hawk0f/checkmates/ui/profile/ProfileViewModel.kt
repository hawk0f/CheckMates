package dev.hawk0f.checkmates.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import dev.hawk0f.checkmates.shared.protocol.ProfileResponse
import dev.hawk0f.checkmates.shared.protocol.UpdateProfileRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val history: List<GameHistoryItem> = emptyList(),
    val historyLoaded: Boolean = false
)

class ProfileViewModel : ViewModel() {

    val profile: StateFlow<ProfileResponse?> = AuthManager.profile
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthManager.profile.value)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        if (AuthManager.isLoggedIn) {
            loadHistory()
        }
    }

    fun login(login: String, password: String) = authAction {
        AuthManager.login(login, password)
    }

    fun register(login: String, password: String, displayName: String) = authAction {
        AuthManager.register(login, password, displayName)
    }

    fun updateDisplayName(name: String) = authAction {
        AuthManager.updateProfile(UpdateProfileRequest(displayName = name))
    }

    fun updateAvatar(kind: String, value: String) = authAction {
        AuthManager.updateProfile(UpdateProfileRequest(avatarKind = kind, avatarValue = value))
    }

    fun logout() {
        AuthManager.logout()
        _uiState.value = ProfileUiState()
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun loadHistory() {
        val token = AuthManager.token ?: return
        viewModelScope.launch {
            runCatching { AuthManager.api.gamesHistory(token) }
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(history = response.games, historyLoaded = true)
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        historyLoaded = true,
                        error = throwable.message ?: "failed to load history"
                    )
                }
        }
    }

    private fun authAction(block: suspend () -> Unit) {
        _uiState.value = _uiState.value.copy(busy = true, error = null)
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(busy = false)
                    loadHistory()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        error = throwable.message ?: "request failed"
                    )
                }
        }
    }
}

object ReplayHolder {
    var current: GameHistoryItem? = null
}
