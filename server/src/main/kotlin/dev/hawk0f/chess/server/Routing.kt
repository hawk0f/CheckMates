package dev.hawk0f.chess.server

import dev.hawk0f.chess.shared.domain.PieceColor
import dev.hawk0f.chess.shared.protocol.GameMessage
import dev.hawk0f.chess.shared.protocol.ProtocolJson
import dev.hawk0f.chess.shared.protocol.ShortCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.webSocket
import dev.hawk0f.chess.shared.protocol.CreateGameRequest
import dev.hawk0f.chess.shared.protocol.CreateGameResponse
import dev.hawk0f.chess.shared.protocol.GameInfoResponse
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

fun Application.configureRouting(registry: RoomRegistry) {
    routing {
        get("/health") {
            call.respondText("ok")
        }

        post("/api/games") {
            val request = call.receive<CreateGameRequest>()
            val hostName = request.hostName.trim().take(30).ifEmpty { "Host" }
            val created = registry.create(hostName)
            call.respond(
                CreateGameResponse(
                    gameId = created.gameId,
                    shortCode = created.shortCode,
                    joinUrl = registry.joinUrl(created.shortCode),
                    playerToken = created.playerToken
                )
            )
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
                    hostName = room?.hostName
                )
            )
        }

        webSocket("/ws/game/{gameId}") {
            val gameId = call.parameters["gameId"].orEmpty()
            val room = registry.byId(gameId)
            if (room == null) {
                sendMessage(GameMessage.ProtocolError("GAME_NOT_FOUND", "game expired or never existed"))
                return@webSocket
            }
            val token = call.request.queryParameters["token"]
            var color: PieceColor? = null
            if (token != null) {
                color = room.attach(token, this)
                if (color == null) {
                    sendMessage(GameMessage.ProtocolError("BAD_TOKEN", "unknown player token"))
                    return@webSocket
                }
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
                    color = dispatch(room, color, message) ?: continue
                }
            } finally {
                color?.let { room.detach(it) }
            }
        }
    }
}

private suspend fun WebSocketServerSession.dispatch(
    room: GameRoom,
    color: PieceColor?,
    message: GameMessage
): PieceColor? {
    if (color != null) {
        room.handle(color, message)
        return color
    }
    return when (message) {
        is GameMessage.JoinGame -> {
            val guestName = message.playerName.trim().take(30).ifEmpty { "Guest" }
            val guestToken = java.util.UUID.randomUUID().toString()
            val assigned = room.join(guestToken, guestName, this)
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
                assigned
            }
        }

        is GameMessage.Reconnect -> {
            val assigned = room.attach(message.playerToken, this)
            if (assigned == null) {
                sendMessage(GameMessage.ProtocolError("BAD_TOKEN", "unknown player token"))
            }
            assigned
        }

        else -> {
            sendMessage(GameMessage.ProtocolError("NOT_JOINED", "join or reconnect first"))
            null
        }
    }
}
