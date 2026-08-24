package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.protocol.ProtocolJson
import dev.hawk0f.checkmates.shared.protocol.ShortCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.webSocket
import dev.hawk0f.checkmates.shared.protocol.CreateGameRequest
import dev.hawk0f.checkmates.shared.protocol.CreateGameResponse
import dev.hawk0f.checkmates.shared.protocol.GameInfoResponse
import dev.hawk0f.checkmates.shared.protocol.GameSpeed
import dev.hawk0f.checkmates.shared.protocol.LeaderboardResponse
import dev.hawk0f.checkmates.shared.protocol.SeekJson
import dev.hawk0f.checkmates.shared.protocol.SeekMessage
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import kotlinx.coroutines.CancellationException
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

fun Application.configureRouting(
    registry: RoomRegistry,
    users: UserRepository,
    ratings: RatingRepository? = null,
    seekPool: SeekPool? = null
) {
    routing {
        accountRoutes(users, ratings)

        get("/health") {
            call.respondText("ok")
        }

        rateLimit(RoomRateLimit) {
            post("/api/games") {
                val request = call.receive<CreateGameRequest>()
                val hostName = request.hostName.trim().take(30).ifEmpty { "Host" }
                val hostUserId = call.bearerToken()?.let { users.userIdByToken(it) }
                val created = registry.create(
                    hostName = hostName,
                    timeControl = request.timeControl?.takeIf { it.isSupported() },
                    hostUserId = hostUserId
                )
                call.respond(
                    CreateGameResponse(
                        gameId = created.gameId,
                        shortCode = created.shortCode,
                        joinUrl = registry.joinUrl(created.shortCode),
                        playerToken = created.playerToken
                    )
                )
            }
        }

        wellKnownRoutes()

        get("/game/{code}") {
            val code = ShortCode.normalize(call.parameters["code"].orEmpty())
            val room = if (ShortCode.isValid(code)) registry.byCode(code) else null
            call.respondLandingPage(code, room?.hostName)
        }

        get("/api/games/{code}") {
            val code = call.parameters["code"].orEmpty()
            val room = if (ShortCode.isValid(ShortCode.normalize(code))) registry.byCode(code) else null
            call.respond(
                GameInfoResponse(
                    exists = room != null,
                    joinable = room?.status == RoomStatus.WAITING_FOR_GUEST,
                    gameId = room?.gameId,
                    hostName = room?.hostName,
                    timeControl = room?.timeControl
                )
            )
        }

        if (ratings != null) {
            get("/api/leaderboard") {
                val speed = GameSpeed.byId(call.request.queryParameters["speed"]) ?: GameSpeed.BLITZ
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                call.respond(LeaderboardResponse(speed, ratings.leaderboard(speed, limit)))
            }
        }

        if (seekPool != null) {
            webSocket("/ws/seek") {
                val initialSeconds = call.request.queryParameters["initial"]?.toIntOrNull()
                val incrementSeconds = call.request.queryParameters["increment"]?.toIntOrNull() ?: 0
                if (initialSeconds == null || initialSeconds !in 10..86400 || incrementSeconds !in 0..600) {
                    sendSeek(SeekMessage.Error("BAD_TIME_CONTROL", "unsupported time control"))
                    return@webSocket
                }
                val timeControl = TimeControl(initialSeconds, incrementSeconds)
                val userId = call.request.queryParameters["token"]?.let { users.userIdByToken(it) }
                val name = call.request.queryParameters["name"]?.trim()?.take(30).orEmpty().ifEmpty { "Player" }
                val speed = GameSpeed.of(timeControl)
                val rating = if (userId != null && ratings != null) {
                    ratings.ratingValue(userId, speed)
                } else {
                    Glicko2.DEFAULT_RATING.toInt()
                }
                val (seekId, result) = seekPool.enqueue(name, userId, timeControl, rating)
                try {
                    if (!result.isCompleted) {
                        sendSeek(SeekMessage.Waiting(seekPool.queuedFor(timeControl), rating, speed))
                    }
                    val matched = result.await()
                    sendSeek(matched)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    sendSeek(SeekMessage.Error("SEEK_FAILED", error.message ?: "seek failed"))
                } finally {
                    seekPool.cancel(seekId)
                }
            }
        }

        webSocket("/ws/game/{gameId}") {
            val gameId = call.parameters["gameId"].orEmpty()
            val room = registry.byId(gameId)
            if (room == null) {
                sendMessage(GameMessage.ProtocolError("GAME_NOT_FOUND", "game expired or never existed"))
                return@webSocket
            }
            val queryToken = call.request.queryParameters["token"]
            var token: String? = null
            if (queryToken != null) {
                if (room.attach(queryToken, this) == null) {
                    sendMessage(GameMessage.ProtocolError("BAD_TOKEN", "unknown player token"))
                    return@webSocket
                }
                token = queryToken
            }
            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) {
                        continue
                    }
                    val message = runCatching { ProtocolJson.decode(frame.readText()) }.getOrNull()
                    if (message == null) {
                        sendMessage(GameMessage.ProtocolError("BAD_MESSAGE", "cannot parse message"))
                        continue
                    }
                    token = dispatch(room, token, message, users) ?: continue
                }
            } finally {
                token?.let { room.detach(it) }
            }
        }
    }
}

private suspend fun WebSocketServerSession.dispatch(
    room: GameRoom,
    token: String?,
    message: GameMessage,
    users: UserRepository
): String? {
    if (token != null) {
        room.handle(token, message)
        return token
    }
    return when (message) {
        is GameMessage.JoinGame -> {
            val guestName = message.playerName.trim().take(30).ifEmpty { "Guest" }
            val guestToken = java.util.UUID.randomUUID().toString()
            val guestUserId = message.authToken?.let { users.userIdByToken(it) }
            val assigned = room.join(guestToken, guestName, this, guestUserId)
            if (assigned == null) {
                sendMessage(GameMessage.ProtocolError("NOT_JOINABLE", "game already started or finished"))
                null
            } else {
                sendMessage(
                    GameMessage.GameCreated(
                        gameId = room.gameId,
                        shortCode = room.shortCode,
                        joinUrl = "",
                        playerToken = guestToken
                    )
                )
                guestToken
            }
        }

        is GameMessage.Reconnect -> {
            val assigned = room.attach(message.playerToken, this)
            if (assigned == null) {
                sendMessage(GameMessage.ProtocolError("BAD_TOKEN", "unknown player token"))
                null
            } else {
                message.playerToken
            }
        }

        else -> {
            sendMessage(GameMessage.ProtocolError("NOT_JOINED", "join or reconnect first"))
            null
        }
    }
}

private suspend fun WebSocketServerSession.sendSeek(message: SeekMessage) {
    send(Frame.Text(SeekJson.encode(message)))
}

private fun TimeControl.isSupported(): Boolean {
    if (initialSeconds !in 10..86400 || incrementSeconds !in 0..600) {
        return false
    }
    val blackInitial = blackInitialSeconds
    if (blackInitial != null && blackInitial !in 10..86400) {
        return false
    }
    val blackIncrement = blackIncrementSeconds
    return blackIncrement == null || blackIncrement in 0..600
}
