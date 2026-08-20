package dev.hawk0f.checkmates.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _recent = MutableStateFlow<List<GameHistoryItem>>(emptyList())
    val recent: StateFlow<List<GameHistoryItem>> = _recent.asStateFlow()

    init {
        loadRecent()
    }

    fun loadRecent() {
        val token = AuthManager.token ?: return
        viewModelScope.launch {
            runCatching { AuthManager.api.gamesHistory(token) }
                .onSuccess { response -> _recent.value = response.games.take(3) }
        }
    }
}
