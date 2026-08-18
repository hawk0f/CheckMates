package dev.hawk0f.chess.server

import dev.hawk0f.chess.shared.domain.ChessGame
import dev.hawk0f.chess.shared.domain.GameOverReason
import dev.hawk0f.chess.shared.domain.MoveOutcome
import dev.hawk0f.chess.shared.domain.PieceColor
import dev.hawk0f.chess.shared.protocol.GameMessage
import dev.hawk0f.chess.shared.protocol.ProtocolJson
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class RoomStatus {
    WAITING_FOR_GUEST,
    IN_PROGRESS,
    FINISHED
}

class PlayerSlot(
    val token: String,
    val name: String,
    var session: WebSocketServerSession?
)

class GameRoom(
    val gameId: String,
    val shortCode: String,
    hostToken: String,
    hostName: String,
    hostColor: PieceColor
) {

    private val game = ChessGame()
    private val mutex = Mutex()
    private val players = mutableMapOf<PieceColor, PlayerSlot>()

    var status: RoomStatus = RoomStatus.WAITING_FOR_GUEST
        private set
    var lastActivityMillis: Long = System.currentTimeMillis()
        private set
    private var drawOfferedBy: PieceColor? = null

    init {
        players[hostColor] = PlayerSlot(hostToken, hostName, null)
    }

    val hostName: String get() = players.values.first().name

    suspend fun attach(token: String, session: WebSocketServerSession): PieceColor? = mutex.withLock {
        touch()
        val entry = players.entries.find { it.value.token == token } ?: return null
        entry.value.session = session
        session.sendMessage(GameMessage.ColorAssigned(entry.key))
        session.sendMessage(resyncMessage())
        opponentOf(entry.key)?.let { opponent ->
            opponent.session?.sendMessage(GameMessage.OpponentConnectionChanged(connected = true))
            session.sendMessage(GameMessage.OpponentJoined(opponent.name))
        }
        entry.key
    }

    suspend fun join(guestToken: String, guestName: String, session: WebSocketServerSession): PieceColor? = mutex.withLock {
        touch()
        if (status != RoomStatus.WAITING_FOR_GUEST) {
            return null
        }
        val hostColor = players.keys.first()
        val guestColor = hostColor.opposite
        players[guestColor] = PlayerSlot(guestToken, guestName, session)
        status = RoomStatus.IN_PROGRESS
        session.sendMessage(GameMessage.ColorAssigned(guestColor))
        session.sendMessage(GameMessage.OpponentJoined(players[hostColor]!!.name))
        session.sendMessage(resyncMessage())
        players[hostColor]!!.session?.sendMessage(GameMessage.OpponentJoined(guestName))
        guestColor
    }

    suspend fun handle(color: PieceColor, message: GameMessage) {
        mutex.withLock {
            touch()
            when (message) {
                is GameMessage.MakeMove -> handleMove(color, message.uci)
                GameMessage.Resign -> finishLocked(GameOverReason.RESIGNATION, color.opposite)
                GameMessage.OfferDraw -> {
                    if (status == RoomStatus.IN_PROGRESS && drawOfferedBy == null) {
                        drawOfferedBy = color
                        opponentOf(color)?.session?.sendMessage(GameMessage.DrawOffered)
                    }
                }
                GameMessage.AcceptDraw -> {
                    if (drawOfferedBy == color.opposite) {
                        finishLocked(GameOverReason.DRAW_AGREED, null)
                    }
                }
                GameMessage.DeclineDraw -> {
                    if (drawOfferedBy == color.opposite) {
                        drawOfferedBy = null
                        opponentOf(color)?.session?.sendMessage(GameMessage.DrawDeclined)
                    }
                }
                GameMessage.RequestResync -> players[color]?.session?.sendMessage(resyncMessage())
                GameMessage.Ping -> players[color]?.session?.sendMessage(GameMessage.Pong)
                else -> players[color]?.session?.sendMessage(
                    GameMessage.ProtocolError("UNEXPECTED_MESSAGE", "unexpected message type")
                )
            }
        }
    }

    suspend fun detach(color: PieceColor) {
        mutex.withLock {
            touch()
            players[color]?.session = null
            opponentOf(color)?.session?.sendMessage(GameMessage.OpponentConnectionChanged(connected = false))
        }
    }

    fun isStale(nowMillis: Long): Boolean {
        val idleMillis = nowMillis - lastActivityMillis
        return when (status) {
            RoomStatus.FINISHED -> idleMillis > 60 * 60 * 1000
            RoomStatus.WAITING_FOR_GUEST -> idleMillis > 2 * 60 * 60 * 1000
            RoomStatus.IN_PROGRESS -> idleMillis > 24 * 60 * 60 * 1000
        }
    }

    suspend fun closeSessions() {
        mutex.withLock {
            for (slot in players.values) {
                runCatching { slot.session?.close() }
                slot.session = null
            }
        }
    }

    private suspend fun handleMove(color: PieceColor, uci: String) {
        if (status != RoomStatus.IN_PROGRESS) {
            players[color]?.session?.sendMessage(GameMessage.MoveRejected(uci, "GAME_NOT_ACTIVE"))
            return
        }
        if (game.sideToMove() != color) {
            players[color]?.session?.sendMessage(GameMessage.MoveRejected(uci, "NOT_YOUR_TURN"))
            return
        }
        when (val outcome = game.applyUci(uci)) {
            is MoveOutcome.Applied -> {
                drawOfferedBy = null
                val state = outcome.state
                broadcast(GameMessage.MoveApplied(uci, state.fen, state.uciHistory.size))
                state.result?.let { result ->
                    status = RoomStatus.FINISHED
                    broadcast(GameMessage.GameOver(result.reason, result.winner))
                }
            }
            MoveOutcome.Illegal -> players[color]?.session?.sendMessage(GameMessage.MoveRejected(uci, "ILLEGAL"))
        }
    }

    private suspend fun finishLocked(reason: GameOverReason, winner: PieceColor?) {
        if (status == RoomStatus.FINISHED) {
            return
        }
        game.finish(reason, winner)
        status = RoomStatus.FINISHED
        broadcast(GameMessage.GameOver(reason, winner))
    }

    private fun resyncMessage() = GameMessage.Resync(
        fen = game.fen(),
        uciHistory = game.state().uciHistory,
        drawOfferPending = drawOfferedBy != null
    )

    private suspend fun broadcast(message: GameMessage) {
        for (slot in players.values) {
            slot.session?.sendMessage(message)
        }
    }

    private fun opponentOf(color: PieceColor): PlayerSlot? = players[color.opposite]

    private fun touch() {
        lastActivityMillis = System.currentTimeMillis()
    }
}

suspend fun WebSocketServerSession.sendMessage(message: GameMessage) {
    runCatching { send(Frame.Text(ProtocolJson.encode(message))) }
}
