package dev.hawk0f.checkmates.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.session.GameSessionHolder
import dev.hawk0f.checkmates.session.OnlineGameResume
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ResumableGame(val opponentName: String?, val live: Boolean)

class HomeViewModel : ViewModel() {

    private val _recent = MutableStateFlow<List<GameHistoryItem>>(emptyList())
    val recent: StateFlow<List<GameHistoryItem>> = _recent.asStateFlow()

    private val _resumable = MutableStateFlow(currentResumable())
    val resumable: StateFlow<ResumableGame?> = _resumable.asStateFlow()

    private val _resuming = MutableStateFlow(false)
    val resuming: StateFlow<Boolean> = _resuming.asStateFlow()

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

    fun refreshResumable() {
        _resumable.value = currentResumable()
    }

    fun resumeGame(onReady: () -> Unit) {
        if (GameSessionHolder.current != null) {
            onReady()
            return
        }
        if (_resuming.value) {
            return
        }
        _resuming.value = true
        viewModelScope.launch {
            val restored = runCatching { OnlineGameResume.resume() }.getOrDefault(false)
            _resuming.value = false
            _resumable.value = currentResumable()
            if (restored) {
                onReady()
            }
        }
    }

    private fun currentResumable(): ResumableGame? {
        val live = GameSessionHolder.current
        if (live != null) {
            return ResumableGame(opponentName = live.opponentName.value, live = true)
        }
        val saved = OnlineGameResume.stored() ?: return null
        return ResumableGame(opponentName = saved.opponentName, live = false)
    }
}
