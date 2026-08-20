package dev.hawk0f.checkmates.net.lichess

import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import dev.hawk0f.checkmates.shared.transport.GameTransport
import dev.hawk0f.checkmates.shared.transport.TransportConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

class LichessGameTransport(
    private val api: LichessApi,
    private val token: String,
    val gameId: String,
    private val myUsername: String
) : GameTransport {

    private val _incoming = MutableSharedFlow<GameMessage>(extraBufferCapacity = 64)
    private val _connectionState =
        MutableStateFlow<TransportConnectionState>(TransportConnectionState.Connecting)

    private var job: Job? = null
    private var closedByUser = false
    private var finished = false
    private var myColor: PieceColor? = null
    private var timeControl: TimeControl? = null
    private var moves = emptyList<String>()
    private var mirror = ChessGame()
    private var whiteMillis: Long? = null
    private var blackMillis: Long? = null
    private var drawOfferSeen = false

    override val incoming: Flow<GameMessage> = _incoming
    override val connectionState: StateFlow<TransportConnectionState> = _connectionState.asStateFlow()

    fun start(scope: CoroutineScope) {
        if (job != null) {
            return
        }
        job = scope.launch {
            var attempt = 0
            while (!closedByUser && !finished) {
                try {
                    api.gameStream(token, gameId).collect { event ->
                        attempt = 0
                        _connectionState.value = TransportConnectionState.Connected
                        handleEvent(event)
                    }
                } catch (_: Exception) {
                }
                if (closedByUser || finished) {
                    break
                }
                _connectionState.value = TransportConnectionState.Reconnecting
                attempt++
                if (attempt > 8) {
                    break
                }
                delay(minOf(1000L shl (attempt - 1), 15_000L))
            }
            if (!closedByUser && !finished) {
                _connectionState.value = TransportConnectionState.Closed("connection lost")
            }
        }
    }

    private suspend fun handleEvent(event: JsonObject) {
        when (event.typeName()) {
            "gameFull" -> handleGameFull(event)
            "gameState" -> handleGameState(event, resyncing = false)
            else -> {}
        }
    }

    private suspend fun handleGameFull(event: JsonObject) {
        val white = event.objectAt("white")
        val black = event.objectAt("black")
        val color = if (white.matchesMe()) PieceColor.WHITE else PieceColor.BLACK
        myColor = color
        val clock = event.objectAt("clock")
        timeControl = clock?.let {
            val initial = it.longAt("initial") ?: return@let null
            TimeControl(
                initialSeconds = (initial / 1000).toInt(),
                incrementSeconds = ((it.longAt("increment") ?: 0L) / 1000).toInt()
            )
        }
        _incoming.emit(GameMessage.ColorAssigned(color))
        val opponent = if (color == PieceColor.WHITE) black else white
        _incoming.emit(GameMessage.OpponentJoined(opponent.displayName()))
        val state = event.objectAt("state")
        if (state != null) {
            handleGameState(state, resyncing = true)
        }
    }

    private suspend fun handleGameState(state: JsonObject, resyncing: Boolean) {
        val incomingMoves = state.stringAt("moves")
            ?.split(' ')
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        whiteMillis = state.longAt("wtime") ?: whiteMillis
        blackMillis = state.longAt("btime") ?: blackMillis
        val appended = !resyncing &&
            incomingMoves.size >= moves.size &&
            incomingMoves.take(moves.size) == moves

        if (appended) {
            for (index in moves.size until incomingMoves.size) {
                val uci = incomingMoves[index]
                mirror.applyUci(uci)
                _incoming.emit(
                    GameMessage.MoveApplied(
                        uci = uci,
                        fenAfter = mirror.fen(),
                        moveNumber = index + 1,
                        whiteMillis = whiteMillis,
                        blackMillis = blackMillis
                    )
                )
            }
        } else {
            rebuildMirror(incomingMoves)
            emitResync()
        }
        moves = incomingMoves

        val opponentOffersDraw = if (myColor == PieceColor.WHITE) {
            state.boolAt("bdraw")
        } else {
            state.boolAt("wdraw")
        } == true
        if (opponentOffersDraw && !drawOfferSeen) {
            drawOfferSeen = true
            _incoming.emit(GameMessage.DrawOffered)
        }
        if (!opponentOffersDraw) {
            drawOfferSeen = false
        }

        val status = state.stringAt("status")
        if (status != null && status !in ACTIVE_STATUSES) {
            finished = true
            val winner = when (state.stringAt("winner")) {
                "white" -> PieceColor.WHITE
                "black" -> PieceColor.BLACK
                else -> null
            }
            _incoming.emit(GameMessage.GameOver(reasonOf(status, winner), winner))
        }
    }

    private fun rebuildMirror(target: List<String>) {
        mirror = ChessGame()
        for (uci in target) {
            mirror.applyUci(uci)
        }
    }

    private suspend fun emitResync() {
        _incoming.emit(
            GameMessage.Resync(
                fen = mirror.fen(),
                uciHistory = moves,
                drawOfferPending = false,
                timeControl = timeControl,
                whiteMillis = whiteMillis,
                blackMillis = blackMillis
            )
        )
    }

    override suspend fun send(message: GameMessage) {
        when (message) {
            is GameMessage.MakeMove -> {
                if (!api.move(token, gameId, message.uci)) {
                    _incoming.emit(GameMessage.MoveRejected(message.uci, "lichess rejected the move"))
                }
            }

            GameMessage.Resign -> {
                if (moves.size < 2) {
                    api.abort(token, gameId)
                } else {
                    api.resign(token, gameId)
                }
            }

            GameMessage.OfferDraw, GameMessage.AcceptDraw -> api.handleDraw(token, gameId, accept = true)
            GameMessage.DeclineDraw -> api.handleDraw(token, gameId, accept = false)
            GameMessage.RequestResync -> if (moves.isNotEmpty() || myColor != null) {
                rebuildMirror(moves)
                emitResync()
            }

            else -> {}
        }
    }

    override suspend fun close() {
        closedByUser = true
        job?.cancel()
        _connectionState.value = TransportConnectionState.Closed(null)
    }

    private fun JsonObject?.matchesMe(): Boolean {
        val id = this?.stringAt("id") ?: return false
        return id.lowercase() == myUsername.lowercase()
    }

    private fun JsonObject?.displayName(): String {
        if (this == null) {
            return "Opponent"
        }
        stringAt("name")?.let { return it }
        intAt("aiLevel")?.let { return "Stockfish level $it" }
        return "Opponent"
    }

    private fun reasonOf(status: String, winner: PieceColor?): GameOverReason = when (status) {
        "mate" -> GameOverReason.CHECKMATE
        "resign" -> GameOverReason.RESIGNATION
        "stalemate" -> GameOverReason.STALEMATE
        "outoftime" -> GameOverReason.TIMEOUT
        "timeout" -> GameOverReason.TIMEOUT
        "draw" -> GameOverReason.DRAW_AGREED
        "aborted" -> GameOverReason.DISCONNECTION
        "noStart" -> GameOverReason.DISCONNECTION
        else -> if (winner == null) GameOverReason.DRAW_AGREED else GameOverReason.RESIGNATION
    }

    private companion object {
        val ACTIVE_STATUSES = setOf("created", "started")
    }
}
