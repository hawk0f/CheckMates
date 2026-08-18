package dev.hawk0f.chess.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.chess.session.ActiveGameSession
import dev.hawk0f.chess.session.GameSessionHolder
import dev.hawk0f.chess.shared.domain.ChessGame
import dev.hawk0f.chess.shared.domain.GameOverReason
import dev.hawk0f.chess.shared.domain.GameState
import dev.hawk0f.chess.shared.domain.MoveOutcome
import dev.hawk0f.chess.shared.domain.PieceColor
import dev.hawk0f.chess.shared.domain.PieceKind
import dev.hawk0f.chess.shared.domain.Square
import dev.hawk0f.chess.shared.protocol.GameMessage
import dev.hawk0f.chess.shared.transport.TransportConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val connectionState: TransportConnectionState?
)

class GameViewModel(private val mode: GameMode) : ViewModel() {

    private var game = ChessGame()

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        if (mode is GameMode.Remote) {
            observeSession(mode.session)
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
    }

    private fun handleServerMessage(message: GameMessage) {
        when (message) {
            is GameMessage.MoveApplied -> {
                if (game.applyUci(message.uci) is MoveOutcome.Illegal) {
                    rebuildFromFen(message.fenAfter)
                }
                _uiState.value = _uiState.value.copy(
                    gameState = game.state(),
                    selected = null,
                    legalTargets = emptySet(),
                    pendingPromotion = null,
                    drawOfferIncoming = false,
                    drawOfferOutgoing = false
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
                    drawOfferIncoming = message.drawOfferPending && _uiState.value.drawOfferIncoming,
                    selected = null,
                    legalTargets = emptySet()
                )
            }

            is GameMessage.GameOver -> {
                game.finish(message.reason, message.winner)
                _uiState.value = _uiState.value.copy(
                    gameState = game.state(),
                    drawOfferIncoming = false,
                    drawOfferOutgoing = false
                )
            }

            is GameMessage.MoveRejected -> {
                viewModelScope.launch { (mode as GameMode.Remote).session.send(GameMessage.RequestResync) }
            }

            GameMessage.DrawOffered -> {
                _uiState.value = _uiState.value.copy(drawOfferIncoming = true)
            }

            GameMessage.DrawDeclined -> {
                _uiState.value = _uiState.value.copy(drawOfferOutgoing = false)
            }

            is GameMessage.OpponentConnectionChanged -> {
                _uiState.value = _uiState.value.copy(opponentConnected = message.connected)
            }

            else -> {}
        }
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
            _uiState.value = initialUiState()
        }
    }

    val isRemote: Boolean get() = mode is GameMode.Remote

    private fun submitMove(uci: String) {
        when (mode) {
            GameMode.Hotseat -> {
                when (game.applyUci(uci)) {
                    is MoveOutcome.Applied -> _uiState.value = clearedSelection()
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
            connectionState = remote?.session?.transport?.connectionState?.value
        )
    }

    override fun onCleared() {
        if (mode is GameMode.Remote) {
            GameSessionHolder.clear()
        }
    }
}
