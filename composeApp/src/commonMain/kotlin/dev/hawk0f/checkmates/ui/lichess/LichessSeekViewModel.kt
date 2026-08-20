package dev.hawk0f.checkmates.ui.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.net.lichess.LichessChallenge
import dev.hawk0f.checkmates.net.lichess.LichessSessionStarter
import dev.hawk0f.checkmates.net.lichess.objectAt
import dev.hawk0f.checkmates.net.lichess.stringAt
import dev.hawk0f.checkmates.net.lichess.typeName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

data class LichessClockOption(
    val label: String,
    val minutes: Int,
    val incrementSeconds: Int,
    val days: Int? = null
) {
    val limitSeconds: Int get() = minutes * 60
    val isCorrespondence: Boolean get() = days != null
}

enum class SeekMode {
    POOL,
    FRIEND,
    AI
}

sealed interface LichessStep {
    data object Idle : LichessStep
    data class Seeking(val label: String) : LichessStep
    data class Waiting(val label: String) : LichessStep
    data object GameReady : LichessStep
    data class Failed(val message: String) : LichessStep
}

data class LichessSeekUiState(
    val mode: SeekMode = SeekMode.POOL,
    val step: LichessStep = LichessStep.Idle,
    val poolClock: LichessClockOption = POOL_CLOCKS[0],
    val directClock: LichessClockOption = DIRECT_CLOCKS[0],
    val friendName: String = "",
    val aiLevel: Int = 6,
    val rated: Boolean = false,
    val ratingSpread: Int = 100,
    val myRating: Int? = null,
    val outgoing: List<LichessChallenge> = emptyList(),
    val openChallengeUrl: String? = null
)

val POOL_CLOCKS = listOf(
    LichessClockOption("3+8", 3, 8),
    LichessClockOption("5+5", 5, 5),
    LichessClockOption("8+0", 8, 0),
    LichessClockOption("10+0", 10, 0),
    LichessClockOption("15+10", 15, 10),
    LichessClockOption("2d", 0, 0, days = 2)
)

val DIRECT_CLOCKS = listOf(
    LichessClockOption("3+2", 3, 2),
    LichessClockOption("5+0", 5, 0),
    LichessClockOption("10+0", 10, 0),
    LichessClockOption("15+10", 15, 10)
)

val AI_LEVELS = listOf(1, 2, 3, 4, 5, 6, 7, 8)

val RATING_SPREADS = listOf(50, 100, 200, 0)

class LichessSeekViewModel : ViewModel() {

    private val api = LichessAuth.api
    private val _uiState = MutableStateFlow(LichessSeekUiState())
    val uiState: StateFlow<LichessSeekUiState> = _uiState.asStateFlow()

    private var eventJob: Job? = null
    private var seekJob: Job? = null
    private var pendingChallengeId: String? = null
    private var awaitingGame = false

    init {
        startEventStream()
        refresh()
    }

    fun onModeChange(mode: SeekMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun onPoolClockChange(option: LichessClockOption) {
        _uiState.value = _uiState.value.copy(poolClock = option)
    }

    fun onDirectClockChange(option: LichessClockOption) {
        _uiState.value = _uiState.value.copy(directClock = option)
    }

    fun onFriendNameChange(value: String) {
        _uiState.value = _uiState.value.copy(friendName = value.trim().take(30))
    }

    fun onAiLevelChange(level: Int) {
        _uiState.value = _uiState.value.copy(aiLevel = level)
    }

    fun onRatedChange(rated: Boolean) {
        _uiState.value = _uiState.value.copy(rated = rated)
    }

    fun onRatingSpreadChange(spread: Int) {
        _uiState.value = _uiState.value.copy(ratingSpread = spread)
    }

    fun start() {
        when (_uiState.value.mode) {
            SeekMode.POOL -> seek()
            SeekMode.FRIEND -> challengeFriend()
            SeekMode.AI -> playComputer()
        }
    }

    fun createOpenChallenge() {
        val token = LichessAuth.token ?: return
        val option = _uiState.value.directClock
        viewModelScope.launch {
            runCatching {
                api.openChallenge(
                    token = token,
                    clockLimitSeconds = option.limitSeconds,
                    incrementSeconds = option.incrementSeconds,
                    rated = _uiState.value.rated
                )
            }.onSuccess { created ->
                _uiState.value = _uiState.value.copy(
                    openChallengeUrl = created.url ?: created.urlWhite
                )
            }.onFailure { fail(it.message ?: "could not create the link") }
        }
    }

    fun cancelChallenge(id: String) {
        val token = LichessAuth.token ?: return
        _uiState.value = _uiState.value.copy(
            outgoing = _uiState.value.outgoing.filterNot { it.id == id }
        )
        viewModelScope.launch {
            runCatching { api.cancelChallenge(token, id) }
        }
    }

    fun cancelWaiting() {
        val token = LichessAuth.token
        awaitingGame = false
        seekJob?.cancel()
        seekJob = null
        val challengeId = pendingChallengeId
        pendingChallengeId = null
        if (token != null && challengeId != null) {
            viewModelScope.launch { runCatching { api.cancelChallenge(token, challengeId) } }
        }
        _uiState.value = _uiState.value.copy(step = LichessStep.Idle)
    }

    fun consumeGameReady() {
        _uiState.value = _uiState.value.copy(step = LichessStep.Idle)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(step = LichessStep.Idle)
    }

    fun dismissOpenChallenge() {
        _uiState.value = _uiState.value.copy(openChallengeUrl = null)
    }

    private fun refresh() {
        val token = LichessAuth.token ?: return
        viewModelScope.launch {
            val challenges = runCatching { api.challenges(token) }.getOrNull()
            val account = runCatching { api.account(token) }.getOrNull()
            _uiState.value = _uiState.value.copy(
                outgoing = challenges?.outgoing ?: _uiState.value.outgoing,
                myRating = account?.perfs?.get("rapid")?.rating ?: _uiState.value.myRating
            )
        }
    }

    private fun seek() {
        val token = LichessAuth.token ?: return
        val state = _uiState.value
        val option = state.poolClock
        awaitingGame = true
        _uiState.value = state.copy(step = LichessStep.Seeking(option.label))
        seekJob?.cancel()
        seekJob = viewModelScope.launch {
            if (option.isCorrespondence) {
                runCatching { api.seekCorrespondence(token, option.days ?: 2, state.rated) }
                    .onFailure { fail(it.message ?: "seek failed") }
                return@launch
            }
            while (awaitingGame) {
                val result = runCatching {
                    api.seek(
                        token = token,
                        minutes = option.minutes,
                        incrementSeconds = option.incrementSeconds,
                        rated = state.rated,
                        ratingRange = ratingRangeOf(state)
                    )
                }
                if (!awaitingGame) {
                    break
                }
                if (result.isFailure) {
                    fail(result.exceptionOrNull()?.message ?: "seek failed")
                    break
                }
                delay(1000)
            }
        }
    }

    private fun challengeFriend() {
        val token = LichessAuth.token ?: return
        val state = _uiState.value
        if (state.friendName.isBlank()) {
            fail("Enter a lichess username")
            return
        }
        awaitingGame = true
        _uiState.value = state.copy(step = LichessStep.Waiting("Waiting for ${state.friendName}"))
        viewModelScope.launch {
            runCatching {
                api.challengeUser(
                    token = token,
                    username = state.friendName,
                    clockLimitSeconds = state.directClock.limitSeconds,
                    incrementSeconds = state.directClock.incrementSeconds,
                    rated = state.rated
                )
            }.onSuccess {
                pendingChallengeId = it
                refresh()
            }.onFailure { fail(it.message ?: "challenge failed") }
        }
    }

    private fun playComputer() {
        val token = LichessAuth.token ?: return
        val state = _uiState.value
        awaitingGame = true
        _uiState.value = state.copy(step = LichessStep.Waiting("Starting game"))
        viewModelScope.launch {
            runCatching {
                api.challengeAi(
                    token,
                    state.aiLevel,
                    state.directClock.limitSeconds,
                    state.directClock.incrementSeconds
                )
            }.onSuccess { openGame(it) }
                .onFailure { fail(it.message ?: "could not start the game") }
        }
    }

    private fun ratingRangeOf(state: LichessSeekUiState): String? {
        val rating = state.myRating ?: return null
        if (state.ratingSpread <= 0) {
            return null
        }
        return "${rating - state.ratingSpread}-${rating + state.ratingSpread}"
    }

    private fun startEventStream() {
        val token = LichessAuth.token ?: return
        if (eventJob?.isActive == true) {
            return
        }
        eventJob = viewModelScope.launch {
            while (LichessAuth.token != null) {
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
                }
            }

            "challenge", "challengeDeclined", "challengeCanceled" -> {
                val id = event.objectAt("challenge")?.stringAt("id")
                if (id != null && id == pendingChallengeId && event.typeName() != "challenge") {
                    pendingChallengeId = null
                    awaitingGame = false
                    _uiState.value = _uiState.value.copy(step = LichessStep.Failed("Challenge declined"))
                }
                refresh()
            }

            else -> {}
        }
    }

    private fun openGame(gameId: String) {
        awaitingGame = false
        if (LichessSessionStarter.open(gameId)) {
            _uiState.value = _uiState.value.copy(step = LichessStep.GameReady)
        } else {
            fail("sign in first")
        }
    }

    private fun fail(message: String) {
        awaitingGame = false
        _uiState.value = _uiState.value.copy(step = LichessStep.Failed(message))
    }
}
