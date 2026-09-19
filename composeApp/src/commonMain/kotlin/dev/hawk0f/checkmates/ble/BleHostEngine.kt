package dev.hawk0f.checkmates.ble

import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.PremovePlanner
import dev.hawk0f.checkmates.shared.protocol.BleCodec
import dev.hawk0f.checkmates.shared.protocol.ClockRules
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import dev.hawk0f.checkmates.shared.transport.GameTransport
import dev.hawk0f.checkmates.shared.transport.TransportConnectionState
import dev.hawk0f.checkmates.platform.epochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class BleHostEngine(
    private val peripheral: BlePeripheralServer,
    private val hostName: String,
    private val timeControl: TimeControl? = null
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var game = ChessGame()
    private val hostColor: PieceColor = if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
    private var guestName: String? = null
    private var drawOfferedBy: PieceColor? = null
    private var takebackOfferedBy: PieceColor? = null
    private var finished = false
    private var whiteMillis = timeControl?.let { ClockRules.initialMillis(it, PieceColor.WHITE) }
    private var blackMillis = timeControl?.let { ClockRules.initialMillis(it, PieceColor.BLACK) }
    private var turnStartedAtMillis = epochMillis()
    private val premoves = mutableMapOf<PieceColor, List<String>>()

    private val localIncoming = MutableSharedFlow<GameMessage>(extraBufferCapacity = 64)
    private val localConnectionState = MutableStateFlow<TransportConnectionState>(TransportConnectionState.Connecting)

    val localTransport: GameTransport = object : GameTransport {
        override val incoming: Flow<GameMessage> = localIncoming
        override val connectionState: StateFlow<TransportConnectionState> = localConnectionState.asStateFlow()

        override suspend fun send(message: GameMessage) {
            handleFrom(hostColor, message)
        }

        override suspend fun close() {
            peripheral.stop()
            scope.cancel()
            localConnectionState.value = TransportConnectionState.Closed(null)
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
                    localConnectionState.value = TransportConnectionState.Connected
                } else if (guestJoined.value) {
                    localIncoming.emit(GameMessage.OpponentConnectionChanged(connected = false))
                }
            }
        }
        scope.launch {
            while (true) {
                delay(100)
                checkTimeout()
            }
        }
    }

    private suspend fun handleGuestMessage(message: GameMessage) {
        if (message is GameMessage.JoinGame) {
            guestName = message.playerName.ifBlank { "Guest" }
            sendToGuest(GameMessage.ColorAssigned(hostColor.opposite))
            sendToGuest(GameMessage.ClockConfigured(timeControl))
            clockSnapshot()?.let { sendToGuest(it) }
            sendToGuest(GameMessage.OpponentJoined(hostName))
            localIncoming.emit(GameMessage.ColorAssigned(hostColor))
            localIncoming.emit(GameMessage.ClockConfigured(timeControl))
            clockSnapshot()?.let { localIncoming.emit(it) }
            localIncoming.emit(GameMessage.OpponentJoined(guestName!!))
            turnStartedAtMillis = epochMillis()
            guestJoined.value = true
            return
        }
        handleFrom(hostColor.opposite, message)
    }

    private suspend fun handleFrom(sender: PieceColor, message: GameMessage) {
        when (message) {
            is GameMessage.MakeMove -> {
                premoves.remove(sender)
                handleMove(sender, message.uci)
            }
            is GameMessage.SetPremoves -> setPremoves(sender, message.uciMoves)
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
            GameMessage.OfferTakeback -> {
                if (!finished && takebackOfferedBy == null && game.state().uciHistory.isNotEmpty()) {
                    takebackOfferedBy = sender
                    deliverTo(sender.opposite, GameMessage.TakebackOffered)
                }
            }
            GameMessage.AcceptTakeback -> {
                if (takebackOfferedBy == sender.opposite) {
                    applyTakeback(sender.opposite)
                }
            }
            GameMessage.DeclineTakeback -> {
                if (takebackOfferedBy == sender.opposite) {
                    takebackOfferedBy = null
                    deliverTo(sender.opposite, GameMessage.TakebackDeclined)
                }
            }
            is GameMessage.SendChat -> {
                val clean = message.text.trim().take(140)
                if (clean.isNotEmpty()) {
                    val author = if (sender == hostColor) hostName else guestName ?: "Guest"
                    deliverTo(sender.opposite, GameMessage.ChatSaid(author = author, text = clean))
                    deliverTo(sender, GameMessage.ChatSaid(author = author, text = clean))
                }
            }
            GameMessage.RequestResync -> {
                if (sender == hostColor) {
                    localIncoming.emit(resyncMessage())
                } else {
                    sendToGuest(GameMessage.ClockConfigured(timeControl))
                    clockSnapshot()?.let { sendToGuest(it) }
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
        if (hasFlagged(sender)) {
            finish(GameOverReason.TIMEOUT, sender.opposite)
            return
        }
        when (val outcome = game.applyUci(uci)) {
            is MoveOutcome.Applied -> {
                chargeClock(sender)
                drawOfferedBy = null
                takebackOfferedBy = null
                broadcast(
                    GameMessage.MoveApplied(
                        uci,
                        outcome.state.fen,
                        outcome.state.uciHistory.size,
                        whiteMillis,
                        blackMillis
                    )
                )
                clockSnapshot()?.let { sendToGuest(it) }
                outcome.state.result?.let { result ->
                    finished = true
                    broadcast(GameMessage.GameOver(result.reason, result.winner))
                } ?: runPremoves()
            }
            MoveOutcome.Illegal -> deliverTo(sender, GameMessage.MoveRejected(uci, "ILLEGAL"))
        }
    }

    private suspend fun applyTakeback(requester: PieceColor) {
        takebackOfferedBy = null
        val history = game.state().uciHistory
        if (history.isEmpty()) {
            return
        }
        val drop = if (game.sideToMove() == requester) 2 else 1
        val kept = history.dropLast(drop.coerceAtMost(history.size))
        val rebuilt = ChessGame()
        for (uci in kept) {
            rebuilt.applyUci(uci)
        }
        game = rebuilt
        premoves.clear()
        turnStartedAtMillis = epochMillis()
        drawOfferedBy = null
        localIncoming.emit(resyncMessage())
        sendToGuest(GameMessage.TakebackApplied(history.size - kept.size))
        clockSnapshot()?.let { sendToGuest(it) }
    }

    private suspend fun finish(reason: GameOverReason, winner: PieceColor?) {
        if (finished) {
            return
        }
        finished = true
        premoves.clear()
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

    private fun chargeClock(color: PieceColor, elapsedOverrideMillis: Long? = null) {
        val control = timeControl ?: return
        val now = epochMillis()
        val elapsed = elapsedOverrideMillis ?: (now - turnStartedAtMillis).coerceAtLeast(0)
        val remaining = if (color == PieceColor.WHITE) whiteMillis else blackMillis
        val updated = ClockRules.remainingAfterMove(remaining ?: 0, elapsed, control, color)
        if (color == PieceColor.WHITE) {
            whiteMillis = updated.coerceAtLeast(0)
        } else {
            blackMillis = updated.coerceAtLeast(0)
        }
        turnStartedAtMillis = now
    }

    private fun hasFlagged(color: PieceColor): Boolean {
        if (timeControl == null) {
            return false
        }
        val remaining = if (color == PieceColor.WHITE) whiteMillis else blackMillis
        return remaining != null && epochMillis() - turnStartedAtMillis >= remaining
    }

    private suspend fun checkTimeout() {
        if (finished || !guestJoined.value || timeControl == null) {
            return
        }
        val elapsed = (epochMillis() - turnStartedAtMillis).coerceAtLeast(0)
        val toMove = game.sideToMove()
        val remaining = if (toMove == PieceColor.WHITE) whiteMillis else blackMillis
        if (remaining != null && elapsed >= remaining) {
            if (toMove == PieceColor.WHITE) {
                whiteMillis = 0
            } else {
                blackMillis = 0
            }
            clockSnapshot()?.let { broadcast(it) }
            finish(GameOverReason.TIMEOUT, toMove.opposite)
        }
    }

    private fun clockSnapshot(): GameMessage.ClockUpdated? {
        val white = whiteMillis ?: return null
        val black = blackMillis ?: return null
        return GameMessage.ClockUpdated(white, black)
    }

    private fun resyncMessage() = GameMessage.Resync(
        fen = game.fen(),
        uciHistory = game.state().uciHistory,
        drawOfferPending = drawOfferedBy != null,
        timeControl = timeControl,
        whiteMillis = whiteMillis,
        blackMillis = blackMillis
    )

    private suspend fun setPremoves(color: PieceColor, moves: List<String>) {
        if (moves.isEmpty()) {
            premoves.remove(color)
            return
        }
        val planningFen = if (game.sideToMove() == color) {
            game.fen()
        } else {
            PremovePlanner.planningFen(game.fen())
        }
        val planned = planningFen?.let { PremovePlanner.project(it, moves) }
        if (planned == null) {
            premoves.remove(color)
            deliverTo(color, GameMessage.PremovesDropped("INVALID_PLAN"))
            return
        }
        premoves[color] = moves
        runPremoves()
    }

    private suspend fun runPremoves() {
        while (!finished) {
            val color = game.sideToMove()
            val queue = premoves[color] ?: return
            val uci = queue.firstOrNull() ?: return
            if (!PremovePlanner.isPlayableNow(game, uci)) {
                premoves.remove(color)
                deliverTo(color, GameMessage.PremovesDropped("ILLEGAL_MOVE"))
                return
            }
            premoves[color] = queue.drop(1)
            val outcome = game.applyUci(uci) as? MoveOutcome.Applied ?: return
            chargeClock(color, PREMOVE_ELAPSED_MILLIS)
            drawOfferedBy = null
            takebackOfferedBy = null
            broadcast(
                GameMessage.MoveApplied(
                    uci,
                    outcome.state.fen,
                    outcome.state.uciHistory.size,
                    whiteMillis,
                    blackMillis
                )
            )
            clockSnapshot()?.let { sendToGuest(it) }
            val result = outcome.state.result
            if (result != null) {
                finished = true
                broadcast(GameMessage.GameOver(result.reason, result.winner))
            }
        }
    }

    private companion object {
        const val PREMOVE_ELAPSED_MILLIS = 100L
    }
}
