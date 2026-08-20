package dev.hawk0f.checkmates.ui.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.net.lichess.LichessBroadcast
import dev.hawk0f.checkmates.net.lichess.LichessStreamer
import dev.hawk0f.checkmates.net.lichess.intAt
import dev.hawk0f.checkmates.net.lichess.objectAt
import dev.hawk0f.checkmates.net.lichess.stringAt
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

data class WatchPlayer(
    val name: String,
    val title: String? = null,
    val rating: Int? = null,
    val seconds: Int? = null
)

data class LichessWatchUiState(
    val channels: List<String> = emptyList(),
    val channel: String = "best",
    val gameId: String? = null,
    val gameState: GameState? = null,
    val flipped: Boolean = false,
    val white: WatchPlayer? = null,
    val black: WatchPlayer? = null,
    val live: Boolean = false,
    val broadcasts: List<LichessBroadcast> = emptyList(),
    val streamerCount: Int = 0,
    val error: String? = null
)

private val WATCH_CHANNELS = listOf("best", "blitz", "rapid", "classical", "bullet")

class LichessWatchViewModel : ViewModel() {

    private val api = LichessAuth.api
    private val json = Json { ignoreUnknownKeys = true }
    private val _uiState = MutableStateFlow(LichessWatchUiState(channels = WATCH_CHANNELS))
    val uiState: StateFlow<LichessWatchUiState> = _uiState.asStateFlow()

    private var feedJob: Job? = null
    private val board = ChessGame()

    init {
        watch("best")
        loadSideLists()
    }

    fun watch(channel: String) {
        _uiState.value = _uiState.value.copy(channel = channel, live = false)
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            while (true) {
                runCatching {
                    api.channelFeed(channel).collect { handleFeed(it) }
                }.onFailure {
                    _uiState.value = _uiState.value.copy(live = false)
                }
                delay(3000)
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun handleFeed(event: JsonObject) {
        val data = event.objectAt("d") ?: return
        when (event.stringAt("t")) {
            "featured" -> {
                val players = data["players"] as? JsonArray
                val white = players?.mapNotNull { it as? JsonObject }
                    ?.firstOrNull { it.stringAt("color") == "white" }
                val black = players?.mapNotNull { it as? JsonObject }
                    ?.firstOrNull { it.stringAt("color") == "black" }
                applyFen(data.stringAt("fen"))
                _uiState.value = _uiState.value.copy(
                    live = true,
                    gameId = data.stringAt("id"),
                    flipped = data.stringAt("orientation") == "black",
                    white = white?.toWatchPlayer(),
                    black = black?.toWatchPlayer(),
                    gameState = board.state()
                )
            }

            "fen" -> {
                applyFen(data.stringAt("fen"))
                val whiteClock = data.intAt("wc")
                val blackClock = data.intAt("bc")
                _uiState.value = _uiState.value.copy(
                    live = true,
                    gameState = board.state(),
                    white = _uiState.value.white?.copy(seconds = whiteClock),
                    black = _uiState.value.black?.copy(seconds = blackClock)
                )
            }

            else -> {}
        }
    }

    private fun JsonObject.toWatchPlayer(): WatchPlayer {
        val user = objectAt("user")
        return WatchPlayer(
            name = user?.stringAt("name") ?: "Anonymous",
            title = user?.stringAt("title"),
            rating = intAt("rating"),
            seconds = intAt("seconds")
        )
    }

    private fun applyFen(fen: String?) {
        val value = fen ?: return
        val full = if (value.trim().split(' ').size >= 4) value else "$value w - - 0 1"
        board.loadFen(full)
    }

    private fun loadSideLists() {
        viewModelScope.launch {
            runCatching {
                val list = mutableListOf<LichessBroadcast>()
                api.broadcasts(4).collect { raw ->
                    runCatching { json.decodeFromJsonElement(LichessBroadcast.serializer(), raw) }
                        .onSuccess { list.add(it) }
                }
                list
            }.onSuccess { broadcasts ->
                _uiState.value = _uiState.value.copy(broadcasts = broadcasts)
            }
            runCatching { api.liveStreamers() }.onSuccess { streamers: List<LichessStreamer> ->
                _uiState.value = _uiState.value.copy(streamerCount = streamers.size)
            }
        }
    }
}
