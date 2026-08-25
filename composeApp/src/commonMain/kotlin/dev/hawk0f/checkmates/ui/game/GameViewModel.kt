package dev.hawk0f.checkmates.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.platform.currentPushToken
import dev.hawk0f.checkmates.platform.epochMillis
import dev.hawk0f.checkmates.session.ActiveGameSession
import dev.hawk0f.checkmates.net.lichess.LichessGameTransport
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.session.GameSessionHolder
import dev.hawk0f.checkmates.shared.protocol.GameRecordRequest
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PgnBuilder
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.shared.protocol.ClockRules
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.protocol.GameSpeed
import dev.hawk0f.checkmates.shared.transport.TransportConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.hawk0f.checkmates.session.HotseatGameStore
import dev.hawk0f.checkmates.session.SavedHotseatGame
import dev.hawk0f.checkmates.session.HotseatGamePersistence
import dev.hawk0f.checkmates.shared.domain.PremovePlanner
import dev.hawk0f.checkmates.shared.engine.ChessEngine
import dev.hawk0f.checkmates.shared.engine.EngineLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

data class ChatLine(val author: String, val text: String)

sealed interface GameMode {
    data object Hotseat : GameMode

    data class Computer(
        val level: EngineLevel = EngineLevel.DEFAULT,
        val myColor: PieceColor = PieceColor.WHITE
    ) : GameMode
    data class Remote(val session: ActiveGameSession) : GameMode
}

data class RatingChangeUi(val speed: GameSpeed, val before: Int, val after: Int)

data class GameUiState(
    val gameState: GameState,
    val selected: Square?,
    val legalTargets: Set<Square>,
    val pendingPromotion: Pair<Square, Square>?,
    val myColor: PieceColor?,
    val opponentName: String?,
    val opponentConnected: Boolean,
    val drawOfferIncoming: Boolean,
    val drawOfferOutgoing: Boolean,
    val connectionState: TransportConnectionState?,
    val timeControl: TimeControl? = null,
    val whiteMillis: Long? = null,
    val blackMillis: Long? = null,
    val showTimePicker: Boolean = false,
    val rematchOfferIncoming: Boolean = false,
    val rematchOfferOutgoing: Boolean = false,
    val takebackOfferIncoming: Boolean = false,
    val takebackOfferOutgoing: Boolean = false,
    val premoves: List<String> = emptyList(),
    val chat: List<ChatLine> = emptyList(),
    val engineThinking: Boolean = false,
    val hint: String? = null,
    val ratingChange: RatingChangeUi? = null,
    val premoveState: GameState? = null,
    val seriesMyWins: Int = 0,
    val seriesOpponentWins: Int = 0,
    val seriesDraws: Int = 0
)

private const val HINT_DEPTH = 5
private const val MAX_CHAT_LINES = 50
private const val MAX_CHAT_CHARS = 140

class GameViewModel(
    private val mode: GameMode,
    private val savedGames: HotseatGamePersistence = HotseatGameStore,
    private val engineContext: CoroutineContext = Dispatchers.Default,
    private val startFen: String? = null
) : ViewModel() {

    private var game = ChessGame()
    private var recordUploaded = false
    private var hotseatTimeControl: TimeControl? = null
    private var pendingRemoteMove: String? = null
    private var serverSchedulesPremoves = (mode as? GameMode.Remote)?.session?.kind == "online"
    private var premovesSentToServer: List<String> = emptyList()
    private var timeoutClaimed = false
    private var localTurnStartedAtMillis = epochMillis()
    private val engine = ChessEngine()
    private var engineJob: Job? = null

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        if (startFen != null) {
            game.loadFen(startFen)
            _uiState.value = _uiState.value.copy(gameState = game.state())
        }
        when (mode) {
            is GameMode.Remote -> observeSession(mode.session)
            is GameMode.Computer -> {
                _uiState.value = _uiState.value.copy(myColor = mode.myColor, showTimePicker = false)
                maybeStartEngineTurn()
            }
            GameMode.Hotseat -> if (startFen == null) restoreHotseatGame()
        }
        startClockTicker()
    }

    private fun restoreHotseatGame() {
        val saved = savedGames.load() ?: return
        val restored = ChessGame()
        val applied = saved.uciHistory.takeWhile { uci -> restored.applyUci(uci) is MoveOutcome.Applied }
        if (applied.size != saved.uciHistory.size) {
            savedGames.clear()
            return
        }
        game = restored
        hotseatTimeControl = saved.timeControl
        val elapsed = (epochMillis() - saved.savedAtMillis).coerceAtLeast(0)
        val toMove = game.state().sideToMove
        val whiteMillis = saved.whiteMillis?.let {
            if (toMove == PieceColor.WHITE) (it - elapsed).coerceAtLeast(0) else it
        }
        val blackMillis = saved.blackMillis?.let {
            if (toMove == PieceColor.BLACK) (it - elapsed).coerceAtLeast(0) else it
        }
        _uiState.value = _uiState.value.copy(
            gameState = game.state(),
            timeControl = saved.timeControl,
            whiteMillis = whiteMillis,
            blackMillis = blackMillis,
            showTimePicker = false,
            seriesMyWins = saved.seriesWhiteWins,
            seriesOpponentWins = saved.seriesBlackWins,
            seriesDraws = saved.seriesDraws
        )
    }

    private fun persistHotseatGame() {
        if (mode !is GameMode.Hotseat || startFen != null) {
            return
        }
        val state = _uiState.value
        if (state.gameState.result != null || state.gameState.uciHistory.isEmpty()) {
            savedGames.clear()
            return
        }
        savedGames.save(
            SavedHotseatGame(
                uciHistory = state.gameState.uciHistory,
                timeControl = state.timeControl,
                whiteMillis = state.whiteMillis,
                blackMillis = state.blackMillis,
                savedAtMillis = epochMillis(),
                seriesWhiteWins = state.seriesMyWins,
                seriesBlackWins = state.seriesOpponentWins,
                seriesDraws = state.seriesDraws
            )
        )
    }

    fun selectTimeControl(timeControl: TimeControl?) {
        if (mode !is GameMode.Hotseat) {
            return
        }
        hotseatTimeControl = timeControl
        _uiState.value = _uiState.value.copy(
            timeControl = timeControl,
            whiteMillis = timeControl?.let { ClockRules.initialMillis(it, PieceColor.WHITE) },
            blackMillis = timeControl?.let { ClockRules.initialMillis(it, PieceColor.BLACK) },
            showTimePicker = false
        )
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            var last = epochMillis()
            while (true) {
                delay(200)
                val now = epochMillis()
                val delta = now - last
                last = now
                val state = _uiState.value
                if (state.timeControl == null || state.gameState.result != null) {
                    continue
                }
                if (mode is GameMode.Hotseat && state.gameState.uciHistory.isEmpty()) {
                    continue
                }
                val toMove = state.gameState.sideToMove
                val current = if (toMove == PieceColor.WHITE) state.whiteMillis else state.blackMillis
                if (current == null) {
                    continue
                }
                val next = (current - delta).coerceAtLeast(0)
                _uiState.value = if (toMove == PieceColor.WHITE) {
                    state.copy(whiteMillis = next)
                } else {
                    state.copy(blackMillis = next)
                }
                if (next == 0L) {
                    onFlagFall(toMove)
                }
            }
        }
    }

    private fun onFlagFall(color: PieceColor) {
        when (mode) {
            GameMode.Hotseat, is GameMode.Computer -> {
                if (game.state().result == null) {
                    game.finish(GameOverReason.TIMEOUT, color.opposite)
                    _uiState.value = clearedSelection()
                    maybeUploadRecord()
                    persistHotseatGame()
                }
            }
            is GameMode.Remote -> {
                if (!timeoutClaimed) {
                    timeoutClaimed = true
                    viewModelScope.launch { mode.session.send(GameMessage.ClaimTimeout) }
                }
            }
        }
    }

    private fun observeSession(session: ActiveGameSession) {
        viewModelScope.launch {
            session.messages.collect { message -> handleServerMessage(message) }
        }
        (session.transport as? LichessGameTransport)?.let { lichess ->
            viewModelScope.launch {
                lichess.chat.collect { lines ->
                    _uiState.value = _uiState.value.copy(
                        chat = lines.takeLast(MAX_CHAT_LINES).map { ChatLine(it.author, it.text) }
                    )
                }
            }
        }
        viewModelScope.launch {
            session.myColor.collect { color ->
                _uiState.value = _uiState.value.copy(myColor = color)
                playQueuedPremove()
            }
        }
        viewModelScope.launch {
            session.opponentName.collect { name ->
                _uiState.value = _uiState.value.copy(opponentName = name)
            }
        }
        viewModelScope.launch {
            session.transport.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }
        viewModelScope.launch {
            session.send(GameMessage.RequestResync)
        }
        if (session.kind == "online") {
            viewModelScope.launch {
                currentPushToken()?.let { token ->
                    session.send(GameMessage.RegisterPush(token))
                }
            }
        }
    }

    private fun handleServerMessage(message: GameMessage) {
        when (message) {
            is GameMessage.MoveApplied -> {
                val premoveHead = _uiState.value.premoves.firstOrNull()
                pendingRemoteMove = null
                if (game.applyUci(message.uci) is MoveOutcome.Illegal) {
                    rebuildFromFen(message.fenAfter)
                }
                timeoutClaimed = false
                _uiState.value = _uiState.value.copy(
                    gameState = game.state(),
                    selected = null,
                    legalTargets = emptySet(),
                    pendingPromotion = null,
                    drawOfferIncoming = false,
                    drawOfferOutgoing = false,
                    takebackOfferIncoming = false,
                    takebackOfferOutgoing = false,
                    whiteMillis = message.whiteMillis ?: _uiState.value.whiteMillis,
                    blackMillis = message.blackMillis ?: _uiState.value.blackMillis
                )
                val myColor = _uiState.value.myColor
                if (premoveHead == message.uci && myColor != null && game.sideToMove() != myColor) {
                    premovesSentToServer = premovesSentToServer.drop(1)
                    setPremoves(_uiState.value.premoves.drop(1), notifyServer = false)
                }
            }

            is GameMessage.Resync -> {
                pendingRemoteMove = null
                game = ChessGame()
                var replayFailed = false
                for (uci in message.uciHistory) {
                    if (game.applyUci(uci) is MoveOutcome.Illegal) {
                        replayFailed = true
                        break
                    }
                }
                if (replayFailed || game.fen() != message.fen) {
                    rebuildFromFen(message.fen)
                }
                val missedResult = message.resultReason?.takeIf { game.state().result == null }
                if (missedResult != null) {
                    game.finish(missedResult, message.resultWinner)
                }
                _uiState.value = _uiState.value.copy(
                    gameState = game.state(),
                    takebackOfferIncoming = false,
                    takebackOfferOutgoing = false,
                    drawOfferIncoming = message.drawOfferPending && _uiState.value.drawOfferIncoming,
                    selected = null,
                    legalTargets = emptySet(),
                    premoves = if (missedResult != null) emptyList() else _uiState.value.premoves,
                    premoveState = if (missedResult != null) null else _uiState.value.premoveState,
                    timeControl = message.timeControl ?: _uiState.value.timeControl,
                    whiteMillis = message.whiteMillis ?: _uiState.value.whiteMillis,
                    blackMillis = message.blackMillis ?: _uiState.value.blackMillis
                )
                if (missedResult != null) {
                    maybeUploadRecord()
                }
            }

            is GameMessage.GameOver -> {
                game.finish(message.reason, message.winner)
                _uiState.value = _uiState.value.copy(
                    gameState = game.state(),
                    premoves = emptyList(),
                    premoveState = null,
                    drawOfferIncoming = false,
                    drawOfferOutgoing = false
                )
                maybeUploadRecord()
            }

            is GameMessage.MoveRejected -> {
                pendingRemoteMove = null
                viewModelScope.launch { (mode as GameMode.Remote).session.send(GameMessage.RequestResync) }
            }

            GameMessage.DrawOffered -> {
                _uiState.value = _uiState.value.copy(drawOfferIncoming = true)
            }

            GameMessage.TakebackOffered -> {
                _uiState.value = _uiState.value.copy(takebackOfferIncoming = true)
            }

            GameMessage.TakebackDeclined -> {
                _uiState.value = _uiState.value.copy(takebackOfferOutgoing = false)
            }

            is GameMessage.TakebackApplied -> {
                rebuildWithoutLastPlies(message.plies)
            }

            GameMessage.DrawDeclined -> {
                _uiState.value = _uiState.value.copy(drawOfferOutgoing = false)
            }

            GameMessage.RematchOffered -> {
                _uiState.value = _uiState.value.copy(rematchOfferIncoming = true)
            }

            GameMessage.RematchDeclined -> {
                _uiState.value = _uiState.value.copy(rematchOfferOutgoing = false)
            }

            is GameMessage.RematchStarted -> {
                resetForRematch(message.color)
            }

            is GameMessage.ChatSaid -> {
                val author = message.author.ifBlank {
                    _uiState.value.opponentName ?: "Opponent"
                }
                val line = ChatLine(author = author, text = message.text)
                _uiState.value = _uiState.value.copy(
                    chat = (_uiState.value.chat + line).takeLast(MAX_CHAT_LINES)
                )
            }

            is GameMessage.RatingChanged -> {
                _uiState.value = _uiState.value.copy(
                    ratingChange = RatingChangeUi(message.speed, message.before, message.after)
                )
            }

            is GameMessage.PremovesDropped -> {
                premovesSentToServer = emptyList()
                setPremoves(emptyList(), notifyServer = false)
            }

            is GameMessage.ProtocolError -> {
                val premovesUnsupported = message.code == "UNEXPECTED_MESSAGE" || message.code == "BAD_MESSAGE"
                if (premovesUnsupported && serverSchedulesPremoves && premovesSentToServer.isNotEmpty()) {
                    serverSchedulesPremoves = false
                    premovesSentToServer = emptyList()
                }
            }

            is GameMessage.OpponentConnectionChanged -> {
                _uiState.value = _uiState.value.copy(opponentConnected = message.connected)
            }

            else -> {}
        }
        if (_uiState.value.premoves.isNotEmpty()) {
            setPremoves(_uiState.value.premoves, notifyServer = false)
        }
        playQueuedPremove()
    }

    private fun maybeUploadRecord() {
        val state = game.state()
        val result = state.result ?: return
        if (recordUploaded) {
            return
        }
        recordUploaded = true
        recordSeriesResult(result.winner)
        val session = (mode as? GameMode.Remote)?.session
        if (session?.kind == "lichess" || session?.kind == "online") {
            return
        }
        val myColor = if (session == null) null else _uiState.value.myColor
        val myName = session?.myName ?: "White"
        val opponent = session?.let { _uiState.value.opponentName ?: "Opponent" } ?: "Black"
        val whiteName = if (myColor == PieceColor.BLACK) opponent else myName
        val blackName = if (myColor == PieceColor.BLACK) myName else opponent
        AuthManager.uploadGameIfLoggedIn(
            GameRecordRequest(
                mode = session?.kind ?: "hotseat",
                myColor = myColor,
                whiteName = whiteName,
                blackName = blackName,
                winner = result.winner,
                reason = result.reason,
                uciHistory = state.uciHistory,
                finishedAtMillis = epochMillis()
            )
        )
    }

    private fun recordSeriesResult(winner: PieceColor?) {
        val perspective = if (mode is GameMode.Remote) _uiState.value.myColor else PieceColor.WHITE
        val current = _uiState.value
        _uiState.value = when (winner) {
            null -> current.copy(seriesDraws = current.seriesDraws + 1)
            perspective -> current.copy(seriesMyWins = current.seriesMyWins + 1)
            else -> current.copy(seriesOpponentWins = current.seriesOpponentWins + 1)
        }
    }

    fun offerRematch() {
        if (mode is GameMode.Remote && !_uiState.value.rematchOfferOutgoing) {
            _uiState.value = _uiState.value.copy(rematchOfferOutgoing = true)
            viewModelScope.launch { mode.session.send(GameMessage.OfferRematch) }
        }
    }

    fun acceptRematch() {
        if (mode is GameMode.Remote) {
            viewModelScope.launch { mode.session.send(GameMessage.AcceptRematch) }
        }
    }

    fun declineRematch() {
        if (mode is GameMode.Remote) {
            _uiState.value = _uiState.value.copy(rematchOfferIncoming = false)
            viewModelScope.launch { mode.session.send(GameMessage.DeclineRematch) }
        }
    }

    private fun resetForRematch(newColor: PieceColor) {
        game = ChessGame()
        recordUploaded = false
        timeoutClaimed = false
        val current = _uiState.value
        _uiState.value = current.copy(
            gameState = game.state(),
            selected = null,
            legalTargets = emptySet(),
            pendingPromotion = null,
            myColor = newColor,
            drawOfferIncoming = false,
            drawOfferOutgoing = false,
            rematchOfferIncoming = false,
            rematchOfferOutgoing = false,
            whiteMillis = current.timeControl?.let { ClockRules.initialMillis(it, PieceColor.WHITE) },
            blackMillis = current.timeControl?.let { ClockRules.initialMillis(it, PieceColor.BLACK) }
        )
    }

    private fun rebuildFromFen(fen: String) {
        game = ChessGame()
        game.loadFen(fen)
        pendingRemoteMove = null
    }

    fun onSquareTap(square: Square) {
        val current = _uiState.value
        if (current.pendingPromotion != null || current.gameState.result != null) {
            return
        }
        if (mode is GameMode.Remote && current.myColor != game.sideToMove()) {
            onPremoveTap(square, current)
            return
        }
        if (mode is GameMode.Computer && game.sideToMove() != mode.myColor) {
            return
        }
        clearHint()
        val selected = current.selected
        when {
            selected == square -> {
                _uiState.value = current.copy(selected = null, legalTargets = emptySet())
            }

            selected != null && square in current.legalTargets -> {
                val target = game.castlingRookSquares(selected)[square] ?: square
                if (game.isPromotionMove(selected, target)) {
                    _uiState.value = current.copy(pendingPromotion = selected to target)
                } else {
                    submitMove("${selected.toUci()}${target.toUci()}")
                }
            }

            game.pieceAt(square)?.color == game.sideToMove() -> {
                _uiState.value = current.copy(
                    selected = square,
                    legalTargets = game.legalDestinations(square) + game.castlingRookSquares(square).keys
                )
            }

            else -> {
                _uiState.value = current.copy(selected = null, legalTargets = emptySet())
            }
        }
    }

    private fun onPremoveTap(square: Square, current: GameUiState) {
        val myColor = current.myColor ?: return
        val planningFen = planningFen() ?: return
        val planned = PremovePlanner.project(planningFen, current.premoves) ?: return
        val selected = current.selected
        when {
            selected == square -> {
                _uiState.value = current.copy(selected = null, legalTargets = emptySet())
            }

            selected != null && square in current.legalTargets -> {
                val target = planned.castlingRookSquares(selected)[square] ?: square
                val promotion = if (planned.isPromotionMove(selected, target)) "q" else ""
                val uci = "${selected.toUci()}${target.toUci()}$promotion"
                if (PremovePlanner.canAppend(planningFen, current.premoves, uci)) {
                    setPremoves(current.premoves + uci)
                } else {
                    _uiState.value = current.copy(selected = null, legalTargets = emptySet())
                }
            }

            planned.pieceAt(square)?.color == myColor -> {
                _uiState.value = current.copy(
                    selected = square,
                    legalTargets = planned.legalDestinations(square) + planned.castlingRookSquares(square).keys
                )
            }

            current.premoves.isNotEmpty() -> {
                setPremoves(emptyList())
            }

            else -> {
                _uiState.value = current.copy(selected = null, legalTargets = emptySet())
            }
        }
    }

    fun clearPremoves() {
        if (_uiState.value.premoves.isNotEmpty()) {
            setPremoves(emptyList())
        }
    }

    private fun planningFen(): String? {
        val myColor = _uiState.value.myColor ?: return null
        val base = ChessGame()
        if (runCatching { base.loadFen(game.fen()) }.isFailure) {
            return null
        }
        pendingRemoteMove?.let { uci ->
            if (base.applyUci(uci) is MoveOutcome.Illegal) {
                return null
            }
        }
        return if (base.sideToMove() == myColor) base.fen() else PremovePlanner.planningFen(base.fen())
    }

    private fun setPremoves(premoves: List<String>, notifyServer: Boolean = true) {
        val live = game.state()
        val projected = if (premoves.isEmpty()) {
            null
        } else {
            planningFen()
                ?.let { planningFen -> PremovePlanner.project(planningFen, premoves) }
                ?.state()
                ?.copy(lastMove = live.lastMove, result = live.result)
        }
        val accepted = if (projected == null) emptyList() else premoves
        _uiState.value = _uiState.value.copy(
            premoves = accepted,
            premoveState = projected,
            selected = null,
            legalTargets = emptySet()
        )
        if (notifyServer || accepted != premoves) {
            sendPremovesToServer(accepted)
        }
    }

    private fun sendPremovesToServer(premoves: List<String>) {
        val remote = mode as? GameMode.Remote ?: return
        if (!serverSchedulesPremoves || premoves == premovesSentToServer) {
            return
        }
        premovesSentToServer = premoves
        viewModelScope.launch { remote.session.send(GameMessage.SetPremoves(premoves)) }
    }

    private fun playQueuedPremove() {
        if (mode !is GameMode.Remote || serverSchedulesPremoves) {
            return
        }
        val current = _uiState.value
        val next = current.premoves.firstOrNull() ?: return
        val myColor = current.myColor
        if (current.gameState.result != null || myColor == null) {
            setPremoves(emptyList())
            return
        }
        if (game.sideToMove() != myColor) {
            return
        }
        if (!PremovePlanner.isPlayableNow(game, next)) {
            setPremoves(emptyList())
            return
        }
        submitMove(next)
        setPremoves(current.premoves.drop(1))
    }

    fun onPromotionChosen(kind: PieceKind) {
        val (from, to) = _uiState.value.pendingPromotion ?: return
        val letter = when (kind) {
            PieceKind.QUEEN -> "q"
            PieceKind.ROOK -> "r"
            PieceKind.BISHOP -> "b"
            PieceKind.KNIGHT -> "n"
            else -> return
        }
        submitMove("${from.toUci()}${to.toUci()}$letter")
    }

    fun onPromotionDismissed() {
        _uiState.value = _uiState.value.copy(pendingPromotion = null, selected = null, legalTargets = emptySet())
    }

    fun resign() {
        when (mode) {
            is GameMode.Computer -> {
                val state = _uiState.value.gameState
                if (state.result != null) {
                    return
                }
                engineJob?.cancel()
                game.finish(GameOverReason.RESIGNATION, mode.myColor.opposite)
                _uiState.value = clearedSelection().copy(engineThinking = false)
                maybeUploadRecord()
            }

            GameMode.Hotseat -> {
                val state = _uiState.value.gameState
                if (state.result != null) {
                    return
                }
                game.finish(GameOverReason.RESIGNATION, state.sideToMove.opposite)
                _uiState.value = clearedSelection()
                maybeUploadRecord()
                persistHotseatGame()
            }

            is GameMode.Remote -> viewModelScope.launch { mode.session.send(GameMessage.Resign) }
        }
    }

    fun offerDraw() {
        if (mode is GameMode.Remote && !_uiState.value.drawOfferOutgoing) {
            _uiState.value = _uiState.value.copy(drawOfferOutgoing = true)
            viewModelScope.launch { mode.session.send(GameMessage.OfferDraw) }
        }
    }

    fun acceptDraw() {
        if (mode is GameMode.Remote) {
            viewModelScope.launch { mode.session.send(GameMessage.AcceptDraw) }
        }
    }

    fun declineDraw() {
        if (mode is GameMode.Remote) {
            _uiState.value = _uiState.value.copy(drawOfferIncoming = false)
            viewModelScope.launch { mode.session.send(GameMessage.DeclineDraw) }
        }
    }

    fun newGame() {
        when (mode) {
            GameMode.Hotseat -> {
                game = ChessGame()
                recordUploaded = false
                val series = _uiState.value
                _uiState.value = initialUiState().copy(
                    timeControl = hotseatTimeControl,
                    whiteMillis = hotseatTimeControl?.let { ClockRules.initialMillis(it, PieceColor.WHITE) },
                    blackMillis = hotseatTimeControl?.let { ClockRules.initialMillis(it, PieceColor.BLACK) },
                    showTimePicker = false,
                    seriesMyWins = series.seriesMyWins,
                    seriesOpponentWins = series.seriesOpponentWins,
                    seriesDraws = series.seriesDraws
                )
                savedGames.clear()
            }

            is GameMode.Computer -> {
                engineJob?.cancel()
                game = ChessGame()
                recordUploaded = false
                val series = _uiState.value
                _uiState.value = initialUiState().copy(
                    myColor = mode.myColor,
                    showTimePicker = false,
                    seriesMyWins = series.seriesMyWins,
                    seriesOpponentWins = series.seriesOpponentWins,
                    seriesDraws = series.seriesDraws
                )
                maybeStartEngineTurn()
            }

            is GameMode.Remote -> Unit
        }
    }

    val isRemote: Boolean get() = mode is GameMode.Remote

    val supportsRematch: Boolean get() = (mode as? GameMode.Remote)?.session?.kind == "online"

    val lichessTransport: LichessGameTransport?
        get() = (mode as? GameMode.Remote)?.session?.transport as? LichessGameTransport

    val supportsTakeback: Boolean
        get() = mode is GameMode.Hotseat || (mode as? GameMode.Remote)?.session != null

    fun offerTakeback() {
        val lichess = lichessTransport
        if (lichess != null) {
            viewModelScope.launch { lichess.offerTakeback() }
            return
        }
        when (mode) {
            GameMode.Hotseat -> undoLocalMove()
            is GameMode.Computer -> {
                engineJob?.cancel()
                val history = game.state().uciHistory
                val plies = if (game.sideToMove() == mode.myColor) 2 else 1
                rebuildWithoutLastPlies(plies.coerceAtMost(history.size))
                _uiState.value = _uiState.value.copy(engineThinking = false, hint = null)
            }
            is GameMode.Remote -> {
                if (_uiState.value.takebackOfferOutgoing || _uiState.value.gameState.uciHistory.isEmpty()) {
                    return
                }
                _uiState.value = _uiState.value.copy(takebackOfferOutgoing = true)
                viewModelScope.launch { mode.session.send(GameMessage.OfferTakeback) }
            }
        }
    }

    fun answerTakeback(accept: Boolean) {
        val lichess = lichessTransport
        if (lichess != null) {
            viewModelScope.launch { lichess.answerTakeback(accept) }
            return
        }
        val session = (mode as? GameMode.Remote)?.session ?: return
        _uiState.value = _uiState.value.copy(takebackOfferIncoming = false)
        viewModelScope.launch {
            session.send(
                if (accept) GameMessage.AcceptTakeback else GameMessage.DeclineTakeback
            )
        }
    }

    private fun undoLocalMove() {
        rebuildWithoutLastPlies(1)
    }

    private fun rebuildWithoutLastPlies(plies: Int) {
        val history = game.state().uciHistory
        if (plies <= 0 || history.isEmpty()) {
            return
        }
        val kept = history.dropLast(plies.coerceAtMost(history.size))
        val rebuilt = ChessGame()
        for (uci in kept) {
            rebuilt.applyUci(uci)
        }
        game = rebuilt
        _uiState.value = _uiState.value.copy(
            gameState = game.state(),
            premoves = emptyList(),
            premoveState = null,
            takebackOfferIncoming = false,
            takebackOfferOutgoing = false,
            selected = null,
            legalTargets = emptySet(),
            pendingPromotion = null
        )
        persistHotseatGame()
    }

    fun claimVictory() {
        val transport = lichessTransport ?: return
        viewModelScope.launch { transport.claimVictory() }
    }

    fun sendChat(text: String) {
        val clean = text.trim().take(MAX_CHAT_CHARS)
        if (clean.isEmpty()) {
            return
        }
        val lichess = lichessTransport
        if (lichess != null) {
            viewModelScope.launch { lichess.sendChat(clean) }
            return
        }
        val remote = mode as? GameMode.Remote ?: return
        viewModelScope.launch { remote.session.send(GameMessage.SendChat(clean)) }
    }

    val supportsChat: Boolean get() = mode is GameMode.Remote

    fun buildPgn(): String {
        val state = game.state()
        val session = (mode as? GameMode.Remote)?.session
        val myColor = if (session == null) null else _uiState.value.myColor
        val myName = session?.myName ?: "White"
        val opponent = session?.let { _uiState.value.opponentName ?: "Opponent" } ?: "Black"
        val whiteName = if (myColor == PieceColor.BLACK) opponent else myName
        val blackName = if (myColor == PieceColor.BLACK) myName else opponent
        return PgnBuilder.build(
            whiteName = whiteName,
            blackName = blackName,
            winner = state.result?.winner,
            reason = state.result?.reason,
            uciHistory = state.uciHistory,
            dateMillis = epochMillis()
        )
    }

    private fun submitMove(uci: String) {
        when (mode) {
            GameMode.Hotseat, is GameMode.Computer -> {
                val mover = game.state().sideToMove
                when (game.applyUci(uci)) {
                    is MoveOutcome.Applied -> {
                        chargeLocalClock(mover)
                        _uiState.value = clearedSelection().copy(hint = null)
                        maybeUploadRecord()
                        persistHotseatGame()
                        maybeStartEngineTurn()
                    }
                    MoveOutcome.Illegal -> _uiState.value = _uiState.value.copy(
                        selected = null,
                        legalTargets = emptySet(),
                        pendingPromotion = null
                    )
                }
            }

            is GameMode.Remote -> {
                pendingRemoteMove = uci
                _uiState.value = _uiState.value.copy(selected = null, legalTargets = emptySet(), pendingPromotion = null)
                viewModelScope.launch { mode.session.send(GameMessage.MakeMove(uci)) }
            }
        }
    }

    private fun chargeLocalClock(color: PieceColor) {
        if (mode is GameMode.Remote) {
            return
        }
        val state = _uiState.value
        val timeControl = state.timeControl ?: return
        val remaining = if (color == PieceColor.WHITE) state.whiteMillis else state.blackMillis
        if (remaining == null) {
            return
        }
        val now = epochMillis()
        val elapsed = (now - localTurnStartedAtMillis).coerceAtLeast(0)
        localTurnStartedAtMillis = now
        val charged = ClockRules
            .remainingAfterMove(remaining + elapsed, elapsed, timeControl, color)
            .coerceAtLeast(0)
        _uiState.value = if (color == PieceColor.WHITE) {
            state.copy(whiteMillis = charged)
        } else {
            state.copy(blackMillis = charged)
        }
    }

    private fun maybeStartEngineTurn() {
        val computer = mode as? GameMode.Computer ?: return
        engineJob?.cancel()
        val state = game.state()
        if (state.result != null || state.sideToMove == computer.myColor) {
            _uiState.value = _uiState.value.copy(engineThinking = false)
            return
        }
        val fen = game.fen()
        _uiState.value = _uiState.value.copy(engineThinking = true)
        engineJob = viewModelScope.launch {
            val move = withContext(engineContext) { engine.bestMove(fen, computer.level) }
            if (move == null || game.fen() != fen) {
                _uiState.value = _uiState.value.copy(engineThinking = false)
                return@launch
            }
            val engineColor = game.state().sideToMove
            game.applyUci(move)
            chargeLocalClock(engineColor)
            _uiState.value = clearedSelection().copy(engineThinking = false, hint = null)
            maybeUploadRecord()
        }
    }

    fun requestHint() {
        if (mode is GameMode.Remote) {
            return
        }
        val state = game.state()
        if (state.result != null) {
            return
        }
        val fen = game.fen()
        viewModelScope.launch {
            val line = withContext(engineContext) { engine.analyse(fen, depth = HINT_DEPTH) }
            if (game.fen() == fen) {
                _uiState.value = _uiState.value.copy(hint = line.bestMove)
            }
        }
    }

    fun clearHint() {
        if (_uiState.value.hint != null) {
            _uiState.value = _uiState.value.copy(hint = null)
        }
    }

    val supportsHint: Boolean get() = mode !is GameMode.Remote

    private fun clearedSelection() = _uiState.value.copy(
        gameState = game.state(),
        selected = null,
        legalTargets = emptySet(),
        pendingPromotion = null
    )

    private fun initialUiState(): GameUiState {
        val remote = mode as? GameMode.Remote
        return GameUiState(
            gameState = game.state(),
            selected = null,
            legalTargets = emptySet(),
            pendingPromotion = null,
            myColor = remote?.session?.myColor?.value,
            opponentName = remote?.session?.opponentName?.value,
            opponentConnected = true,
            drawOfferIncoming = false,
            drawOfferOutgoing = false,
            connectionState = remote?.session?.transport?.connectionState?.value,
            showTimePicker = remote == null
        )
    }

    override fun onCleared() {
        if (mode is GameMode.Remote) {
            GameSessionHolder.clear()
        }
    }
}
