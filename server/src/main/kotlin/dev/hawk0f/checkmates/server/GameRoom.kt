package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.protocol.GameSpeed
import dev.hawk0f.checkmates.shared.protocol.GameRecordRequest
import dev.hawk0f.checkmates.shared.protocol.ProtocolJson
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import dev.hawk0f.checkmates.shared.domain.PremovePlanner

enum class RoomStatus {
    WAITING_FOR_GUEST,
    IN_PROGRESS,
    FINISHED
}

fun interface GameRecorder {
    suspend fun record(userId: Long, request: GameRecordRequest)
}

fun interface RatingUpdater {
    suspend fun apply(
        speed: GameSpeed,
        whiteUserId: Long,
        blackUserId: Long,
        whiteScore: Double
    ): List<RatingChange>
}

class PlayerSlot(
    val token: String,
    val name: String,
    var session: WebSocketServerSession?,
    val userId: Long? = null,
    var pushToken: String? = null
)

class GameRoom(
    val gameId: String,
    val shortCode: String,
    hostToken: String,
    hostName: String,
    hostColor: PieceColor,
    val timeControl: TimeControl? = null,
    hostUserId: Long? = null,
    private val recorder: GameRecorder? = null,
    private val ratings: RatingUpdater? = null,
    private val recorderScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val store: RoomStore = NoopRoomStore
) {

    private var game = ChessGame()
    private val mutex = Mutex()
    private val players = mutableMapOf<PieceColor, PlayerSlot>()

    var status: RoomStatus = RoomStatus.WAITING_FOR_GUEST
        private set
    var lastActivityMillis: Long = System.currentTimeMillis()
        private set
    private var drawOfferedBy: PieceColor? = null
    private var rematchOfferedBy: PieceColor? = null
    private var takebackOfferedBy: PieceColor? = null
    private val remainingMillis = mutableMapOf<PieceColor, Long>()
    private val premoves = mutableMapOf<PieceColor, List<String>>()
    private val chatTimestamps = mutableMapOf<PieceColor, ArrayDeque<Long>>()
    private var turnStartedAtMillis: Long = 0

    init {
        players[hostColor] = PlayerSlot(hostToken, hostName, null, hostUserId)
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

    suspend fun join(
        guestToken: String,
        guestName: String,
        session: WebSocketServerSession,
        guestUserId: Long? = null
    ): PieceColor? = mutex.withLock {
        touch()
        if (status != RoomStatus.WAITING_FOR_GUEST) {
            return null
        }
        val hostColor = players.keys.first()
        val guestColor = hostColor.opposite
        players[guestColor] = PlayerSlot(guestToken, guestName, session, guestUserId)
        status = RoomStatus.IN_PROGRESS
        if (timeControl != null) {
            remainingMillis[PieceColor.WHITE] = timeControl.initialSeconds * 1000L
            remainingMillis[PieceColor.BLACK] = timeControl.initialSeconds * 1000L
            turnStartedAtMillis = System.currentTimeMillis()
        }
        session.sendMessage(GameMessage.ColorAssigned(guestColor))
        session.sendMessage(GameMessage.OpponentJoined(players[hostColor]!!.name))
        session.sendMessage(resyncMessage())
        players[hostColor]!!.session?.sendMessage(GameMessage.OpponentJoined(guestName))
        pushIfOffline(hostColor, "Opponent joined", "$guestName joined your game $shortCode")
        persist()
        guestColor
    }

    suspend fun seatGuest(guestToken: String, guestName: String, guestUserId: Long? = null): PieceColor? =
        mutex.withLock {
            touch()
            if (status != RoomStatus.WAITING_FOR_GUEST) {
                return null
            }
            val hostColor = players.keys.first()
            val guestColor = hostColor.opposite
            players[guestColor] = PlayerSlot(guestToken, guestName, null, guestUserId)
            status = RoomStatus.IN_PROGRESS
            if (timeControl != null) {
                remainingMillis[PieceColor.WHITE] = timeControl.initialSeconds * 1000L
                remainingMillis[PieceColor.BLACK] = timeControl.initialSeconds * 1000L
                turnStartedAtMillis = System.currentTimeMillis()
            }
            persist()
            guestColor
        }

    fun snapshot(): RoomSnapshot = RoomSnapshot(
        gameId = gameId,
        shortCode = shortCode,
        status = status,
        timeControl = timeControl,
        uciHistory = game.state().uciHistory,
        players = players.map { (color, slot) ->
            RoomPlayerSnapshot(
                color = color,
                token = slot.token,
                name = slot.name,
                userId = slot.userId,
                remainingMillis = remainingMillis[color]
            )
        },
        turnStartedAtMillis = turnStartedAtMillis,
        lastActivityMillis = lastActivityMillis
    )

    private fun persist() {
        val snapshot = snapshot()
        recorderScope.launch {
            runCatching {
                if (snapshot.status == RoomStatus.IN_PROGRESS) {
                    store.save(snapshot)
                } else {
                    store.delete(snapshot.gameId)
                }
            }.onFailure { error -> log.error("failed to persist room ${snapshot.gameId}", error) }
        }
    }

    suspend fun restoreFrom(snapshot: RoomSnapshot) = mutex.withLock {
        players.clear()
        for (player in snapshot.players) {
            players[player.color] = PlayerSlot(player.token, player.name, null, player.userId)
            player.remainingMillis?.let { remainingMillis[player.color] = it }
        }
        game = ChessGame()
        for (uci in snapshot.uciHistory) {
            game.applyUci(uci)
        }
        status = snapshot.status
        turnStartedAtMillis = System.currentTimeMillis()
        lastActivityMillis = snapshot.lastActivityMillis
    }

    suspend fun handle(token: String, message: GameMessage) {
        mutex.withLock {
            touch()
            val color = players.entries.find { it.value.token == token }?.key ?: return
            when (message) {
                is GameMessage.MakeMove -> {
                    premoves.remove(color)
                    handleMove(color, message.uci)
                }
                is GameMessage.SetPremoves -> setPremoves(color, message.uciMoves)
                is GameMessage.SendChat -> relayChat(color, message.text)
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
                GameMessage.OfferTakeback -> {
                    if (status == RoomStatus.IN_PROGRESS &&
                        takebackOfferedBy == null &&
                        game.state().uciHistory.isNotEmpty()
                    ) {
                        takebackOfferedBy = color
                        opponentOf(color)?.session?.sendMessage(GameMessage.TakebackOffered)
                        pushIfOffline(
                            color.opposite,
                            "Takeback?",
                            "${players[color]?.name ?: "Opponent"} asks to undo a move in game $shortCode"
                        )
                    }
                }
                GameMessage.AcceptTakeback -> {
                    if (takebackOfferedBy == color.opposite) {
                        applyTakeback(takebackOfferedBy!!)
                    }
                }
                GameMessage.DeclineTakeback -> {
                    if (takebackOfferedBy == color.opposite) {
                        takebackOfferedBy = null
                        opponentOf(color)?.session?.sendMessage(GameMessage.TakebackDeclined)
                    }
                }
                GameMessage.RequestResync -> players[color]?.session?.sendMessage(resyncMessage())
                GameMessage.ClaimTimeout -> handleTimeoutClaim()
                is GameMessage.RegisterPush -> {
                    players[color]?.pushToken = message.token
                }
                GameMessage.OfferRematch -> {
                    if (status == RoomStatus.FINISHED && players.size == 2) {
                        if (rematchOfferedBy == color.opposite) {
                            startRematch()
                        } else if (rematchOfferedBy == null) {
                            rematchOfferedBy = color
                            opponentOf(color)?.session?.sendMessage(GameMessage.RematchOffered)
                            pushIfOffline(
                                color.opposite,
                                "Rematch?",
                                "${players[color]?.name ?: "Opponent"} wants a rematch in game $shortCode"
                            )
                        }
                    }
                }
                GameMessage.AcceptRematch -> {
                    if (status == RoomStatus.FINISHED && rematchOfferedBy == color.opposite) {
                        startRematch()
                    }
                }
                GameMessage.DeclineRematch -> {
                    if (rematchOfferedBy == color.opposite) {
                        rematchOfferedBy = null
                        opponentOf(color)?.session?.sendMessage(GameMessage.RematchDeclined)
                    }
                }
                GameMessage.Ping -> players[color]?.session?.sendMessage(GameMessage.Pong)
                else -> players[color]?.session?.sendMessage(
                    GameMessage.ProtocolError("UNEXPECTED_MESSAGE", "unexpected message type")
                )
            }
            persist()
        }
    }

    suspend fun detach(token: String) {
        mutex.withLock {
            touch()
            val color = players.entries.find { it.value.token == token }?.key ?: return
            players[color]?.session = null
            opponentOf(color)?.session?.sendMessage(GameMessage.OpponentConnectionChanged(connected = false))
        }
    }

    private suspend fun applyTakeback(requester: PieceColor) {
        takebackOfferedBy = null
        premoves.clear()
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
        drawOfferedBy = null
        if (timeControl != null) {
            turnStartedAtMillis = System.currentTimeMillis()
        }
        broadcast(resyncMessage())
    }

    private suspend fun startRematch() {
        game = ChessGame()
        premoves.clear()
        drawOfferedBy = null
        rematchOfferedBy = null
        takebackOfferedBy = null
        status = RoomStatus.IN_PROGRESS
        val swapped = players.entries.associate { (color, slot) -> color.opposite to slot }
        players.clear()
        players.putAll(swapped)
        if (timeControl != null) {
            remainingMillis[PieceColor.WHITE] = timeControl.initialSeconds * 1000L
            remainingMillis[PieceColor.BLACK] = timeControl.initialSeconds * 1000L
            turnStartedAtMillis = System.currentTimeMillis()
        }
        for ((color, slot) in players) {
            slot.session?.sendMessage(GameMessage.RematchStarted(color))
            slot.session?.sendMessage(resyncMessage())
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
        if (timeControl != null && flagIsDown(color)) {
            finishLocked(GameOverReason.TIMEOUT, color.opposite)
            return
        }
        when (val outcome = game.applyUci(uci)) {
            is MoveOutcome.Applied -> {
                drawOfferedBy = null
                takebackOfferedBy = null
                chargeClock(color)
                val state = outcome.state
                broadcast(
                    GameMessage.MoveApplied(
                        uci = uci,
                        fenAfter = state.fen,
                        moveNumber = state.uciHistory.size,
                        whiteMillis = remainingMillis[PieceColor.WHITE],
                        blackMillis = remainingMillis[PieceColor.BLACK]
                    )
                )
                state.result?.let { result ->
                    finishLocked(result.reason, result.winner)
                }
                if (state.result == null) {
                    runPremoves()
                    notifyPlayerToMove()
                }
            }
            MoveOutcome.Illegal -> players[color]?.session?.sendMessage(GameMessage.MoveRejected(uci, "ILLEGAL"))
        }
    }

    private fun pushIfOffline(color: PieceColor, title: String, body: String) {
        val slot = players[color] ?: return
        val pushToken = slot.pushToken ?: return
        if (slot.session != null) {
            return
        }
        FcmSender.send(pushToken, title, body, mapOf("shortCode" to shortCode))
    }

    private suspend fun finishLocked(reason: GameOverReason, winner: PieceColor?) {
        if (status == RoomStatus.FINISHED) {
            return
        }
        game.finish(reason, winner)
        status = RoomStatus.FINISHED
        premoves.clear()
        broadcast(GameMessage.GameOver(reason, winner))
        recordFinishedGame(reason, winner)
        updateRatings(winner)
        val resultText = when (winner) {
            PieceColor.WHITE -> "White wins"
            PieceColor.BLACK -> "Black wins"
            null -> "Draw"
        }
        for (color in players.keys) {
            pushIfOffline(color, "Game over", "$resultText — ${reason.name.lowercase().replace('_', ' ')} in game $shortCode")
        }
    }

    private fun recordFinishedGame(reason: GameOverReason, winner: PieceColor?) {
        val recorder = recorder ?: return
        val history = game.state().uciHistory
        val whiteName = players[PieceColor.WHITE]?.name ?: "White"
        val blackName = players[PieceColor.BLACK]?.name ?: "Black"
        val finishedAt = System.currentTimeMillis()
        val pending = players.mapNotNull { (color, slot) ->
            val userId = slot.userId ?: return@mapNotNull null
            userId to GameRecordRequest(
                mode = "online",
                myColor = color,
                whiteName = whiteName,
                blackName = blackName,
                winner = winner,
                reason = reason,
                uciHistory = history,
                finishedAtMillis = finishedAt
            )
        }
        if (pending.isEmpty()) {
            return
        }
        recorderScope.launch {
            for ((userId, request) in pending) {
                runCatching { recorder.record(userId, request) }
                    .onFailure { error -> log.error("failed to record game $gameId for user $userId", error) }
            }
        }
    }

    private fun updateRatings(winner: PieceColor?) {
        val ratings = ratings ?: return
        val timeControl = timeControl ?: return
        val white = players[PieceColor.WHITE] ?: return
        val black = players[PieceColor.BLACK] ?: return
        val whiteUserId = white.userId ?: return
        val blackUserId = black.userId ?: return
        if (game.state().uciHistory.size < MIN_RATED_PLIES) {
            return
        }
        val whiteScore = when (winner) {
            PieceColor.WHITE -> 1.0
            PieceColor.BLACK -> 0.0
            null -> 0.5
        }
        val speed = GameSpeed.of(timeControl)
        recorderScope.launch {
            runCatching {
                val changes = ratings.apply(speed, whiteUserId, blackUserId, whiteScore)
                for (change in changes) {
                    val slot = if (change.userId == whiteUserId) white else black
                    slot.session?.sendMessage(
                        GameMessage.RatingChanged(change.speed, change.before, change.after)
                    )
                }
            }.onFailure { error -> log.error("failed to update ratings for game $gameId", error) }
        }
    }

    private suspend fun relayChat(color: PieceColor, text: String) {
        val clean = text.trim().take(MAX_CHAT_CHARS)
        if (clean.isEmpty()) {
            return
        }
        val now = System.currentTimeMillis()
        val recent = chatTimestamps.getOrPut(color) { ArrayDeque() }
        while (recent.isNotEmpty() && now - recent.first() > CHAT_WINDOW_MILLIS) {
            recent.removeFirst()
        }
        if (recent.size >= MAX_CHAT_PER_WINDOW) {
            return
        }
        recent.addLast(now)
        broadcast(GameMessage.ChatSaid(author = players[color]?.name ?: "Player", text = clean))
    }

    private suspend fun setPremoves(color: PieceColor, uciMoves: List<String>) {
        if (status != RoomStatus.IN_PROGRESS) {
            premoves.remove(color)
            return
        }
        val requested = uciMoves.take(PremovePlanner.MAX_PREMOVES)
        if (requested.isEmpty()) {
            premoves.remove(color)
            return
        }
        val planningFen = if (game.sideToMove() == color) {
            game.fen()
        } else {
            PremovePlanner.planningFen(game.fen())
        }
        val plan = planningFen?.let { PremovePlanner.project(it, requested) }
        if (plan == null) {
            premoves.remove(color)
            players[color]?.session?.sendMessage(GameMessage.PremovesDropped("INVALID_PLAN"))
            return
        }
        if (game.sideToMove() == color) {
            premoves[color] = requested.drop(1)
            handleMove(color, requested.first())
            return
        }
        premoves[color] = requested
        runPremoves()
    }

    private suspend fun runPremoves(): Boolean {
        var played = false
        while (status == RoomStatus.IN_PROGRESS) {
            val color = game.sideToMove()
            val queue = premoves[color] ?: break
            val next = queue.firstOrNull()
            if (next == null) {
                premoves.remove(color)
                break
            }
            if (!PremovePlanner.isPlayableNow(game, next)) {
                premoves.remove(color)
                players[color]?.session?.sendMessage(GameMessage.PremovesDropped("ILLEGAL_MOVE"))
                break
            }
            val rest = queue.drop(1)
            if (rest.isEmpty()) {
                premoves.remove(color)
            } else {
                premoves[color] = rest
            }
            if (!applyPremove(color, next)) {
                break
            }
            played = true
        }
        return played
    }

    private suspend fun applyPremove(color: PieceColor, uci: String): Boolean {
        if (timeControl != null && flagIsDown(color)) {
            premoves.clear()
            finishLocked(GameOverReason.TIMEOUT, color.opposite)
            return false
        }
        val outcome = game.applyUci(uci)
        if (outcome !is MoveOutcome.Applied) {
            premoves.remove(color)
            players[color]?.session?.sendMessage(GameMessage.PremovesDropped("ILLEGAL_MOVE"))
            return false
        }
        drawOfferedBy = null
        takebackOfferedBy = null
        chargeClock(color, elapsedOverrideMillis = PREMOVE_ELAPSED_MILLIS)
        val state = outcome.state
        broadcast(
            GameMessage.MoveApplied(
                uci = uci,
                fenAfter = state.fen,
                moveNumber = state.uciHistory.size,
                whiteMillis = remainingMillis[PieceColor.WHITE],
                blackMillis = remainingMillis[PieceColor.BLACK]
            )
        )
        state.result?.let { result ->
            finishLocked(result.reason, result.winner)
            return false
        }
        return true
    }

    private fun notifyPlayerToMove() {
        if (status != RoomStatus.IN_PROGRESS) {
            return
        }
        val toMove = game.sideToMove()
        val lastMove = game.state().uciHistory.lastOrNull() ?: return
        pushIfOffline(
            toMove,
            "Your move",
            "${players[toMove.opposite]?.name ?: "Opponent"} played $lastMove in game $shortCode"
        )
    }

    private fun flagIsDown(color: PieceColor): Boolean {
        val remaining = remainingMillis[color] ?: return false
        val elapsed = if (game.sideToMove() == color && status == RoomStatus.IN_PROGRESS) {
            System.currentTimeMillis() - turnStartedAtMillis
        } else {
            0
        }
        return remaining - elapsed <= 0
    }

    private fun chargeClock(color: PieceColor, elapsedOverrideMillis: Long? = null) {
        if (timeControl == null) {
            return
        }
        val now = System.currentTimeMillis()
        val elapsed = elapsedOverrideMillis ?: (now - turnStartedAtMillis)
        val left = (remainingMillis[color] ?: 0) - elapsed + timeControl.incrementSeconds * 1000L
        remainingMillis[color] = left
        turnStartedAtMillis = now
    }

    private suspend fun handleTimeoutClaim() {
        if (status != RoomStatus.IN_PROGRESS || timeControl == null) {
            return
        }
        val toMove = game.sideToMove()
        if (flagIsDown(toMove)) {
            finishLocked(GameOverReason.TIMEOUT, toMove.opposite)
        }
    }

    private fun currentClock(color: PieceColor): Long? {
        val remaining = remainingMillis[color] ?: return null
        val elapsed = if (game.sideToMove() == color && status == RoomStatus.IN_PROGRESS) {
            System.currentTimeMillis() - turnStartedAtMillis
        } else {
            0
        }
        return (remaining - elapsed).coerceAtLeast(0)
    }

    companion object {
        const val PREMOVE_ELAPSED_MILLIS = 100L
        const val MIN_RATED_PLIES = 4
        const val MAX_CHAT_CHARS = 140
        const val MAX_CHAT_PER_WINDOW = 5
        const val CHAT_WINDOW_MILLIS = 10_000L

        private val log = LoggerFactory.getLogger(GameRoom::class.java)
    }

    internal fun resyncMessage() = GameMessage.Resync(
        fen = game.fen(),
        uciHistory = game.state().uciHistory,
        drawOfferPending = drawOfferedBy != null,
        timeControl = timeControl,
        whiteMillis = currentClock(PieceColor.WHITE),
        blackMillis = currentClock(PieceColor.BLACK)
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
