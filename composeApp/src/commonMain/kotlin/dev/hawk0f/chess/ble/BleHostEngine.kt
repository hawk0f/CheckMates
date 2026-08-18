package dev.hawk0f.chess.ble

import dev.hawk0f.chess.shared.domain.ChessGame
import dev.hawk0f.chess.shared.domain.GameOverReason
import dev.hawk0f.chess.shared.domain.MoveOutcome
import dev.hawk0f.chess.shared.domain.PieceColor
import dev.hawk0f.chess.shared.protocol.BleCodec
import dev.hawk0f.chess.shared.protocol.GameMessage
import dev.hawk0f.chess.shared.transport.GameTransport
import dev.hawk0f.chess.shared.transport.TransportConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class BleHostEngine(
    private val peripheral: BlePeripheralServer,
    private val hostName: String
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val game = ChessGame()
    private val hostColor: PieceColor = if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
    private var guestName: String? = null
    private var drawOfferedBy: PieceColor? = null
    private var finished = false

    private val localIncoming = MutableSharedFlow<GameMessage>(extraBufferCapacity = 64)
    private val _connectionState = MutableStateFlow<TransportConnectionState>(TransportConnectionState.Connecting)

    val localTransport: GameTransport = object : GameTransport {
        override val incoming: Flow<GameMessage> = localIncoming
        override val connectionState: StateFlow<TransportConnectionState> = _connectionState.asStateFlow()

        override suspend fun send(message: GameMessage) {
            handleFrom(hostColor, message)
        }

        override suspend fun close() {
            peripheral.stop()
            scope.cancel()
            _connectionState.value = TransportConnectionState.Closed(null)
        }
    }

    val guestJoined = MutableStateFlow(false)

    fun start() {
        peripheral.start(hostName) { game.fen() }
        scope.launch {
            peripheral.incomingWrites.collect { bytes ->
                BleCodec.decodeFromGuest(bytes)?.let { message -> handleGuestMessage(message) }
            }
        }
        scope.launch {
            peripheral.centralConnected.collect { connected ->
                if (connected) {
                    _connectionState.value = TransportConnectionState.Connected
                } else if (guestJoined.value) {
                    localIncoming.emit(GameMessage.OpponentConnectionChanged(connected = false))
                }
            }
        }
    }

    private suspend fun handleGuestMessage(message: GameMessage) {
        if (message is GameMessage.JoinGame) {
            guestName = message.playerName.ifBlank { "Guest" }
            sendToGuest(GameMessage.ColorAssigned(hostColor.opposite))
            sendToGuest(GameMessage.OpponentJoined(hostName))
            localIncoming.emit(GameMessage.ColorAssigned(hostColor))
            localIncoming.emit(GameMessage.OpponentJoined(guestName!!))
            guestJoined.value = true
            return
        }
        handleFrom(hostColor.opposite, message)
    }

    private suspend fun handleFrom(sender: PieceColor, message: GameMessage) {
        when (message) {
            is GameMessage.MakeMove -> handleMove(sender, message.uci)
            GameMessage.Resign -> finish(GameOverReason.RESIGNATION, sender.opposite)
            GameMessage.OfferDraw -> {
                if (!finished && drawOfferedBy == null) {
                    drawOfferedBy = sender
                    deliverTo(sender.opposite, GameMessage.DrawOffered)
                }
            }
            GameMessage.AcceptDraw -> {
                if (drawOfferedBy == sender.opposite) {
                    finish(GameOverReason.DRAW_AGREED, null)
                }
            }
            GameMessage.DeclineDraw -> {
                if (drawOfferedBy == sender.opposite) {
                    drawOfferedBy = null
                    deliverTo(sender.opposite, GameMessage.DrawDeclined)
                }
            }
            GameMessage.RequestResync -> {
                if (sender == hostColor) {
                    localIncoming.emit(GameMessage.Resync(game.fen(), game.state().uciHistory, drawOfferedBy != null))
                }
            }
            else -> {}
        }
    }

    private suspend fun handleMove(sender: PieceColor, uci: String) {
        if (finished || game.sideToMove() != sender) {
            deliverTo(sender, GameMessage.MoveRejected(uci, "NOT_YOUR_TURN"))
            return
        }
        when (val outcome = game.applyUci(uci)) {
            is MoveOutcome.Applied -> {
                drawOfferedBy = null
                broadcast(GameMessage.MoveApplied(uci, outcome.state.fen, outcome.state.uciHistory.size))
                outcome.state.result?.let { result ->
                    finished = true
                    broadcast(GameMessage.GameOver(result.reason, result.winner))
                }
            }
            MoveOutcome.Illegal -> deliverTo(sender, GameMessage.MoveRejected(uci, "ILLEGAL"))
        }
    }

    private suspend fun finish(reason: GameOverReason, winner: PieceColor?) {
        if (finished) {
            return
        }
        finished = true
        game.finish(reason, winner)
        broadcast(GameMessage.GameOver(reason, winner))
    }

    private suspend fun broadcast(message: GameMessage) {
        localIncoming.emit(message)
        sendToGuest(message)
    }

    private suspend fun deliverTo(color: PieceColor, message: GameMessage) {
        if (color == hostColor) {
            localIncoming.emit(message)
        } else {
            sendToGuest(message)
        }
    }

    private suspend fun sendToGuest(message: GameMessage) {
        BleCodec.encodeToGuest(message)?.let { peripheral.notifyGuest(it) }
    }
}
