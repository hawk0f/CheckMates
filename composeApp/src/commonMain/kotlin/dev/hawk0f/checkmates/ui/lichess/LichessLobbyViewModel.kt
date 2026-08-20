package dev.hawk0f.checkmates.ui.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.lichess.LichessApi
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.net.lichess.LichessGameTransport
import dev.hawk0f.checkmates.net.lichess.boolAt
import dev.hawk0f.checkmates.net.lichess.objectAt
import dev.hawk0f.checkmates.net.lichess.stringAt
import dev.hawk0f.checkmates.net.lichess.typeName
import dev.hawk0f.checkmates.session.ActiveGameSession
import dev.hawk0f.checkmates.session.GameSessionHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

data class LichessClockOption(val label: String, val minutes: Int, val incrementSeconds: Int) {
    val limitSeconds: Int get() = minutes * 60
}

data class LichessChallengeItem(val id: String, val from: String, val timeLabel: String)

data class LichessOngoingGame(val gameId: String, val opponent: String)

sealed interface LichessStep {
    data object Idle : LichessStep
    data object Authorizing : LichessStep
    data class Seeking(val label: String) : LichessStep
    data class Waiting(val label: String) : LichessStep
    data object GameReady : LichessStep
    data class Failed(val message: String) : LichessStep
}

data class LichessLobbyUiState(
    val username: String? = null,
    val step: LichessStep = LichessStep.Idle,
    val friendName: String = "",
    val quickClock: LichessClockOption = QUICK_CLOCKS[0],
    val friendClock: LichessClockOption = FRIEND_CLOCKS[0],
    val aiLevel: Int = 3,
    val incoming: List<LichessChallengeItem> = emptyList(),
    val ongoing: List<LichessOngoingGame> = emptyList()
)

val QUICK_CLOCKS = listOf(
    LichessClockOption("10+0", 10, 0),
    LichessClockOption("15+10", 15, 10),
    LichessClockOption("30+0", 30, 0)
)

val FRIEND_CLOCKS = listOf(
    LichessClockOption("3+2", 3, 2),
    LichessClockOption("5+0", 5, 0),
    LichessClockOption("10+0", 10, 0),
    LichessClockOption("15+10", 15, 10)
)

val AI_LEVELS = listOf(1, 3, 5, 8)

class LichessLobbyViewModel : ViewModel() {

    private val api = LichessAuth.api
    private val _uiState = MutableStateFlow(LichessLobbyUiState(username = LichessAuth.username.value))
    val uiState: StateFlow<LichessLobbyUiState> = _uiState.asStateFlow()

    private var eventJob: Job? = null
    private var seekJob: Job? = null
    private var pendingChallengeId: String? = null
    private var awaitingGame = false

    init {
        viewModelScope.launch {
            LichessAuth.refreshUsername()
        }
        viewModelScope.launch {
            LichessAuth.username.collect { name ->
                _uiState.value = _uiState.value.copy(username = name)
                if (name != null) {
                    startEventStream()
                }
            }
        }
    }

    fun startLogin(): String = LichessAuth.buildAuthorizeUrl()

    fun onAuthCallback(code: String?, state: String?, error: String?) {
        if (code == null) {
            _uiState.value = _uiState.value.copy(
                step = LichessStep.Failed(error ?: "authorization was cancelled")
            )
            return
        }
        _uiState.value = _uiState.value.copy(step = LichessStep.Authorizing)
        viewModelScope.launch {
            LichessAuth.completeLogin(code, state)
                .onSuccess { _uiState.value = _uiState.value.copy(step = LichessStep.Idle, username = it) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        step = LichessStep.Failed(it.message ?: "sign in failed")
                    )
                }
        }
    }

    fun logout() {
        seekJob?.cancel()
        eventJob?.cancel()
        LichessAuth.logout()
        _uiState.value = LichessLobbyUiState()
    }

    fun onFriendNameChange(value: String) {
        _uiState.value = _uiState.value.copy(friendName = value.trim().take(30))
    }

    fun onQuickClockChange(option: LichessClockOption) {
        _uiState.value = _uiState.value.copy(quickClock = option)
    }

    fun onFriendClockChange(option: LichessClockOption) {
        _uiState.value = _uiState.value.copy(friendClock = option)
    }

    fun onAiLevelChange(level: Int) {
        _uiState.value = _uiState.value.copy(aiLevel = level)
    }

    fun seek() {
        val token = LichessAuth.token ?: return
        val option = _uiState.value.quickClock
        awaitingGame = true
        _uiState.value = _uiState.value.copy(step = LichessStep.Seeking(option.label))
        seekJob?.cancel()
        seekJob = viewModelScope.launch {
            while (awaitingGame) {
                val kept = runCatching {
                    api.seek(token, option.minutes, option.incrementSeconds, rated = false)
                }
                if (!awaitingGame) {
                    break
                }
                if (kept.isFailure) {
                    fail(kept.exceptionOrNull()?.message ?: "seek failed")
                    break
                }
                delay(1000)
            }
        }
    }

    fun challengeFriend() {
        val token = LichessAuth.token ?: return
        val state = _uiState.value
        val name = state.friendName
        if (name.isBlank()) {
            fail("Enter a lichess username")
            return
        }
        awaitingGame = true
        _uiState.value = state.copy(step = LichessStep.Waiting("Waiting for $name"))
        viewModelScope.launch {
            runCatching {
                api.challengeUser(token, name, state.friendClock.limitSeconds, state.friendClock.incrementSeconds)
            }.onSuccess { pendingChallengeId = it }
                .onFailure { fail(it.message ?: "challenge failed") }
        }
    }

    fun playComputer() {
        val token = LichessAuth.token ?: return
        val state = _uiState.value
        awaitingGame = true
        _uiState.value = state.copy(step = LichessStep.Waiting("Starting game"))
        viewModelScope.launch {
            runCatching {
                api.challengeAi(
                    token,
                    state.aiLevel,
                    state.friendClock.limitSeconds,
                    state.friendClock.incrementSeconds
                )
            }.onSuccess { openGame(it) }
                .onFailure { fail(it.message ?: "could not start the game") }
        }
    }

    fun acceptChallenge(id: String) {
        val token = LichessAuth.token ?: return
        awaitingGame = true
        viewModelScope.launch {
            runCatching { api.acceptChallenge(token, id) }
                .onFailure { fail(it.message ?: "could not accept") }
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

    fun resume(gameId: String) {
        awaitingGame = true
        openGame(gameId)
    }

    fun cancelWaiting() {
        val token = LichessAuth.token
        awaitingGame = false
        seekJob?.cancel()
        seekJob = null
        val challengeId = pendingChallengeId
        pendingChallengeId = null
        if (token != null && challengeId != null) {
            viewModelScope.launch {
                runCatching { api.cancelChallenge(token, challengeId) }
            }
        }
        _uiState.value = _uiState.value.copy(step = LichessStep.Idle)
    }

    fun consumeGameReady() {
        _uiState.value = _uiState.value.copy(step = LichessStep.Idle)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(step = LichessStep.Idle)
    }

    private fun startEventStream() {
        val token = LichessAuth.token ?: return
        if (eventJob?.isActive == true) {
            return
        }
        eventJob = viewModelScope.launch {
            while (true) {
                runCatching {
                    api.eventStream(token).collect { handleEvent(it) }
                }
                if (LichessAuth.token == null) {
                    break
                }
                delay(3000)
            }
        }
    }

    private fun handleEvent(event: JsonObject) {
        when (event.typeName()) {
            "gameStart" -> {
                val game = event.objectAt("game") ?: return
                val gameId = game.stringAt("gameId") ?: game.stringAt("id") ?: return
                if (awaitingGame) {
                    seekJob?.cancel()
                    seekJob = null
                    pendingChallengeId = null
                    openGame(gameId)
                } else {
                    val opponent = game.objectAt("opponent")?.stringAt("username") ?: "Opponent"
                    val known = _uiState.value.ongoing.any { it.gameId == gameId }
                    if (!known) {
                        _uiState.value = _uiState.value.copy(
                            ongoing = _uiState.value.ongoing + LichessOngoingGame(gameId, opponent)
                        )
                    }
                }
            }

            "gameFinish" -> {
                val gameId = event.objectAt("game")?.stringAt("gameId") ?: return
                _uiState.value = _uiState.value.copy(
                    ongoing = _uiState.value.ongoing.filterNot { it.gameId == gameId }
                )
            }

            "challenge" -> {
                val challenge = event.objectAt("challenge") ?: return
                val challenger = challenge.objectAt("challenger")?.stringAt("id")
                val me = _uiState.value.username?.lowercase()
                if (challenger != null && challenger.lowercase() == me) {
                    pendingChallengeId = challenge.stringAt("id")
                    return
                }
                val id = challenge.stringAt("id") ?: return
                val from = challenge.objectAt("challenger")?.stringAt("name") ?: "Someone"
                val label = challenge.objectAt("timeControl")?.stringAt("show")
                    ?: challenge.objectAt("timeControl")?.stringAt("type")
                    ?: "correspondence"
                val rated = challenge.boolAt("rated") == true
                val suffix = if (rated) " · rated" else ""
                if (_uiState.value.incoming.none { it.id == id }) {
                    _uiState.value = _uiState.value.copy(
                        incoming = _uiState.value.incoming +
                            LichessChallengeItem(id, from, label + suffix)
                    )
                }
            }

            "challengeDeclined", "challengeCanceled" -> {
                val id = event.objectAt("challenge")?.stringAt("id") ?: return
                if (id == pendingChallengeId) {
                    pendingChallengeId = null
                    awaitingGame = false
                    _uiState.value = _uiState.value.copy(step = LichessStep.Failed("Challenge declined"))
                }
                _uiState.value = _uiState.value.copy(
                    incoming = _uiState.value.incoming.filterNot { it.id == id }
                )
            }

            else -> {}
        }
    }

    private fun openGame(gameId: String) {
        val token = LichessAuth.token ?: return
        val myName = _uiState.value.username ?: return
        awaitingGame = false
        val transport = LichessGameTransport(
            api = api,
            token = token,
            gameId = gameId,
            myUsername = myName
        )
        val session = ActiveGameSession(transport, kind = "lichess", myName = myName)
        GameSessionHolder.install(session)
        transport.start(session.scope)
        _uiState.value = _uiState.value.copy(step = LichessStep.GameReady)
    }

    private fun fail(message: String) {
        awaitingGame = false
        _uiState.value = _uiState.value.copy(step = LichessStep.Failed(message))
    }
}
