package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.CreateGameRequest
import dev.hawk0f.checkmates.shared.protocol.CreateGameResponse
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.protocol.ProtocolJson
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class PremoveTest {

    private fun testClient(builder: ApplicationTestBuilder): HttpClient = builder.createClient {
        install(ContentNegotiation) {
            json()
        }
        install(WebSockets)
    }

    private suspend fun DefaultClientWebSocketSession.receiveMessage(): GameMessage {
        while (true) {
            val frame = incoming.receive()
            if (frame is Frame.Text) {
                return ProtocolJson.decode(frame.readText())
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendMessage(message: GameMessage) {
        send(Frame.Text(ProtocolJson.encode(message)))
    }

    private suspend fun DefaultClientWebSocketSession.awaitMessage(predicate: (GameMessage) -> Boolean): GameMessage {
        while (true) {
            val message = receiveMessage()
            if (predicate(message)) {
                return message
            }
        }
    }

    @Test
    fun queuedPremovesAreAppliedInstantlyAndCostNoClock() = testApplication {
        application { testModule() }
        val client = testClient(this)
        val created: CreateGameResponse = client.post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody(CreateGameRequest("Host", TimeControl(60, 0)))
        }.body()

        val firstPremove = CompletableDeferred<GameMessage.MoveApplied>()
        val secondPremove = CompletableDeferred<GameMessage.MoveApplied>()

        coroutineScope {
            val hostJob = launch {
                client.webSocket("/ws/game/${created.gameId}?token=${created.playerToken}") {
                    val color = (awaitMessage { it is GameMessage.ColorAssigned } as GameMessage.ColorAssigned).color
                    awaitMessage { it is GameMessage.OpponentJoined }
                    play(color, firstPremove, secondPremove)
                }
            }
            val guestJob = launch {
                client.webSocket("/ws/game/${created.gameId}") {
                    sendMessage(GameMessage.JoinGame(created.shortCode, "Guest"))
                    val color = (awaitMessage { it is GameMessage.ColorAssigned } as GameMessage.ColorAssigned).color
                    play(color, firstPremove, secondPremove)
                }
            }

            withTimeout(20_000) {
                val first = firstPremove.await()
                assertEquals("e7e5", first.uci)
                assertEquals(
                    60_000 - GameRoom.PREMOVE_ELAPSED_MILLIS,
                    first.blackMillis,
                    "a premove must cost exactly the flat premove charge"
                )
                assertTrue(first.whiteMillis!! <= 59_800, "mover should have been charged: ${first.whiteMillis}")

                val second = secondPremove.await()
                assertEquals("g8f6", second.uci)
                assertEquals(
                    60_000 - 2 * GameRoom.PREMOVE_ELAPSED_MILLIS,
                    second.blackMillis,
                    "the second premove must cost the flat charge too"
                )
            }
            hostJob.cancel()
            guestJob.cancel()
        }
    }

    private suspend fun DefaultClientWebSocketSession.play(
        color: PieceColor,
        firstPremove: CompletableDeferred<GameMessage.MoveApplied>,
        secondPremove: CompletableDeferred<GameMessage.MoveApplied>
    ) {
        if (color == PieceColor.BLACK) {
            sendMessage(GameMessage.SetPremoves(listOf("e7e5", "g8f6")))
        } else {
            delay(400)
            sendMessage(GameMessage.MakeMove("e2e4"))
        }
        while (true) {
            val message = receiveMessage()
            if (message !is GameMessage.MoveApplied) {
                continue
            }
            when (message.uci) {
                "e7e5" -> {
                    firstPremove.complete(message)
                    if (color == PieceColor.WHITE) {
                        delay(300)
                        sendMessage(GameMessage.MakeMove("d2d4"))
                    }
                }
                "g8f6" -> {
                    secondPremove.complete(message)
                    return
                }
            }
        }
    }

    @Test
    fun aPremoveTheBoardRejectsDropsTheQueue() = testApplication {
        application { testModule() }
        val client = testClient(this)
        val created: CreateGameResponse = client.post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody(CreateGameRequest("Host"))
        }.body()

        val dropped = CompletableDeferred<GameMessage.PremovesDropped>()

        coroutineScope {
            val hostJob = launch {
                client.webSocket("/ws/game/${created.gameId}?token=${created.playerToken}") {
                    val color = (awaitMessage { it is GameMessage.ColorAssigned } as GameMessage.ColorAssigned).color
                    awaitMessage { it is GameMessage.OpponentJoined }
                    runRejectionScenario(color, dropped)
                }
            }
            val guestJob = launch {
                client.webSocket("/ws/game/${created.gameId}") {
                    sendMessage(GameMessage.JoinGame(created.shortCode, "Guest"))
                    val color = (awaitMessage { it is GameMessage.ColorAssigned } as GameMessage.ColorAssigned).color
                    runRejectionScenario(color, dropped)
                }
            }

            withTimeout(20_000) {
                assertEquals("ILLEGAL_MOVE", dropped.await().reason)
            }
            hostJob.cancel()
            guestJob.cancel()
        }
    }

    private suspend fun DefaultClientWebSocketSession.runRejectionScenario(
        color: PieceColor,
        dropped: CompletableDeferred<GameMessage.PremovesDropped>
    ) {
        if (color == PieceColor.WHITE) {
            sendMessage(GameMessage.MakeMove("e2e4"))
        }
        while (true) {
            val message = receiveMessage()
            if (message is GameMessage.PremovesDropped) {
                dropped.complete(message)
                return
            }
            if (message !is GameMessage.MoveApplied) {
                continue
            }
            when {
                message.uci == "e2e4" && color == PieceColor.BLACK -> {
                    sendMessage(GameMessage.SetPremoves(listOf("d7d5", "d5e4")))
                }

                message.uci == "d7d5" && color == PieceColor.WHITE -> {
                    sendMessage(GameMessage.MakeMove("e4e5"))
                }
            }
        }
    }

    @Test
    fun premovesSentOnYourOwnTurnAreChargedLikeANormalMove() = testApplication {
        application { testModule() }
        val client = testClient(this)
        val created: CreateGameResponse = client.post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody(CreateGameRequest("Host", TimeControl(60, 0)))
        }.body()

        val applied = CompletableDeferred<GameMessage.MoveApplied>()

        coroutineScope {
            val hostJob = launch {
                client.webSocket("/ws/game/${created.gameId}?token=${created.playerToken}") {
                    val color = (awaitMessage { it is GameMessage.ColorAssigned } as GameMessage.ColorAssigned).color
                    awaitMessage { it is GameMessage.OpponentJoined }
                    selfTurnPremove(color, applied)
                }
            }
            val guestJob = launch {
                client.webSocket("/ws/game/${created.gameId}") {
                    sendMessage(GameMessage.JoinGame(created.shortCode, "Guest"))
                    val color = (awaitMessage { it is GameMessage.ColorAssigned } as GameMessage.ColorAssigned).color
                    selfTurnPremove(color, applied)
                }
            }

            withTimeout(20_000) {
                val move = applied.await()
                assertEquals("e2e4", move.uci)
                assertTrue(
                    move.whiteMillis!! <= 60_000 - 400,
                    "a premove played on your own turn must be charged real elapsed time: ${move.whiteMillis}"
                )
            }
            hostJob.cancel()
            guestJob.cancel()
        }
    }

    private suspend fun DefaultClientWebSocketSession.selfTurnPremove(
        color: PieceColor,
        applied: CompletableDeferred<GameMessage.MoveApplied>
    ) {
        if (color == PieceColor.WHITE) {
            delay(500)
            sendMessage(GameMessage.SetPremoves(listOf("e2e4")))
        }
        while (true) {
            val message = receiveMessage()
            if (message is GameMessage.MoveApplied && message.uci == "e2e4") {
                applied.complete(message)
                return
            }
        }
    }
}
