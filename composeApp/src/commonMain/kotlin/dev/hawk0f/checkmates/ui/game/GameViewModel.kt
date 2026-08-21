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
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.transport.TransportConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GameMode {
    data object Hotseat : GameMode
    data class Remote(val session: ActiveGameSession) : GameMode
}

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
    val seriesMyWins: Int = 0,
    val seriesOpponentWins: Int = 0,
    val seriesDraws: Int = 0
)

class GameViewModel(private val mode: GameMode) : ViewModel() {

    private var game = ChessGame()
    private var recordUploaded = false
    private var hotseatTimeControl: TimeControl? = null
    private var timeoutClaimed = false

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        if (mode is GameMode.Remote) {
            observeSession(mode.session)
        }
        startClockTicker()
    }

    fun selectTimeControl(timeControl: TimeControl?) {
        if (mode !is GameMode.Hotseat) {
            return
        }
        hotseatTimeControl = timeControl
        _uiState.value = _uiState.value.copy(
            timeControl = timeControl,
            whiteMillis = timeControl?.let { it.initialSeconds * 1000L },
            blackMillis = timeControl?.let { it.initialSeconds * 1000L },
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
            GameMode.Hotseat -> {
                if (game.state().result == null) {
                    game.finish(GameOverReason.TIMEOUT, color.opposite)
                    _uiState.value = clearedSelection()
                    maybeUploadRecord()
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
        viewModelScope.launch {
            session.myColor.collect { color ->
                _uiState.value = _uiState.value.copy(myColor = color)
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
            }

            is GameMessage.Resync -> {
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
                _uiState.value = _uiState.value.copy(
                    gameState = game.state(),
                    takebackOfferIncoming = false,
                    takebackOfferOutgoing = false,
                    drawOfferIncoming = message.drawOfferPending && _uiState.value.drawOfferIncoming,
                    selected = null,
                    legalTargets = emptySet(),
                    timeControl = message.timeControl ?: _uiState.value.timeControl,
                    whiteMillis = message.whiteMillis ?: _uiState.value.whiteMillis,
                    blackMillis = message.blackMillis ?: _uiState.value.blackMillis
                )
            }

            is GameMessage.GameOver -> {
                game.finish(message.reason, message.winner)
                _uiState.value = _uiState.value.copy(
                    gameState = game.state(),
                    drawOfferIncoming = false,
                    drawOfferOutgoing = false
                )
                maybeUploadRecord()
            }

            is GameMessage.MoveRejected -> {
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

            is GameMessage.OpponentConnectionChanged -> {
                _uiState.value = _uiState.value.copy(opponentConnected = message.connected)
            }

            else -> {}
        }
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
        if (session?.kind == "lichess") {
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
            whiteMillis = current.timeControl?.let { it.initialSeconds * 1000L },
            blackMillis = current.timeControl?.let { it.initialSeconds * 1000L }
        )
    }

    private fun rebuildFromFen(fen: String) {
        game = ChessGame()
        game.loadFen(fen)
    }

    fun onSquareTap(square: Square) {
        val current = _uiState.value
        if (current.pendingPromotion != null || current.gameState.result != null) {
            return
        }
        if (mode is GameMode.Remote && current.myColor != game.sideToMove()) {
            return
        }
        val selected = current.selected
        when {
            selected != null && square in current.legalTargets -> {
                if (game.isPromotionMove(selected, square)) {
                    _uiState.value = current.copy(pendingPromotion = selected to square)
                } else {
                    submitMove("${selected.toUci()}${square.toUci()}")
                }
            }

            game.pieceAt(square)?.color == game.sideToMove() -> {
                _uiState.value = current.copy(selected = square, legalTargets = game.legalDestinations(square))
            }

            else -> {
                _uiState.value = current.copy(selected = null, legalTargets = emptySet())
            }
        }
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
            GameMode.Hotseat -> {
                val state = _uiState.value.gameState
                if (state.result != null) {
                    return
                }
                game.finish(GameOverReason.RESIGNATION, state.sideToMove.opposite)
                _uiState.value = clearedSelection()
                maybeUploadRecord()
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
        if (mode is GameMode.Hotseat) {
            game = ChessGame()
            recordUploaded = false
            val series = _uiState.value
            _uiState.value = initialUiState().copy(
                timeControl = hotseatTimeControl,
                whiteMillis = hotseatTimeControl?.let { it.initialSeconds * 1000L },
                blackMillis = hotseatTimeControl?.let { it.initialSeconds * 1000L },
                showTimePicker = false,
                seriesMyWins = series.seriesMyWins,
                seriesOpponentWins = series.seriesOpponentWins,
                seriesDraws = series.seriesDraws
            )
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
            takebackOfferIncoming = false,
            takebackOfferOutgoing = false,
            selected = null,
            legalTargets = emptySet(),
            pendingPromotion = null
        )
    }

    fun claimVictory() {
        val transport = lichessTransport ?: return
        viewModelScope.launch { transport.claimVictory() }
    }

    fun sendChat(text: String) {
        val transport = lichessTransport ?: return
        if (text.isBlank()) {
            return
        }
        viewModelScope.launch { transport.sendChat(text.trim()) }
    }

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
            GameMode.Hotseat -> {
                when (game.applyUci(uci)) {
                    is MoveOutcome.Applied -> {
                        _uiState.value = clearedSelection()
                        maybeUploadRecord()
                    }
                    MoveOutcome.Illegal -> _uiState.value = _uiState.value.copy(
                        selected = null,
                        legalTargets = emptySet(),
                        pendingPromotion = null
                    )
                }
            }

            is GameMode.Remote -> {
                _uiState.value = _uiState.value.copy(selected = null, legalTargets = emptySet(), pendingPromotion = null)
                viewModelScope.launch { mode.session.send(GameMessage.MakeMove(uci)) }
            }
        }
    }

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
