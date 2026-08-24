package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.CreateGameRequest
import dev.hawk0f.checkmates.shared.protocol.CreateGameResponse
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.protocol.ProtocolJson
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ChatTest {

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
    fun chatIsRelayedToBothPlayersAndRateLimited() = testApplication {
        application { module() }
        val client = testClient(this)
        val created: CreateGameResponse = client.post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody(CreateGameRequest("Host"))
        }.body()

        val hostSaw = CompletableDeferred<GameMessage.ChatSaid>()
        val guestBurst = CompletableDeferred<List<GameMessage.ChatSaid>>()

        coroutineScope {
            val hostJob = launch {
                client.webSocket("/ws/game/${created.gameId}?token=${created.playerToken}") {
                    awaitMessage { it is GameMessage.OpponentJoined }
                    hostSaw.complete(awaitMessage { it is GameMessage.ChatSaid } as GameMessage.ChatSaid)
                }
            }
            val guestJob = launch {
                client.webSocket("/ws/game/${created.gameId}") {
                    sendMessage(GameMessage.JoinGame(created.shortCode, "Guest"))
                    awaitMessage { it is GameMessage.ColorAssigned }
                    repeat(9) { index -> sendMessage(GameMessage.SendChat("line $index")) }
                    sendMessage(GameMessage.Ping)
                    val seen = mutableListOf<GameMessage.ChatSaid>()
                    while (true) {
                        val message = receiveMessage()
                        if (message is GameMessage.ChatSaid) {
                            seen += message
                        }
                        if (message is GameMessage.Pong) {
                            guestBurst.complete(seen)
                            return@webSocket
                        }
                    }
                }
            }

            withTimeout(20_000) {
                val first = hostSaw.await()
                assertEquals("Guest", first.author)
                assertEquals("line 0", first.text)

                val burst = guestBurst.await()
                assertEquals(GameRoom.MAX_CHAT_PER_WINDOW, burst.size, "chat flood must be capped")
                assertTrue(burst.all { it.author == "Guest" })
            }
            hostJob.cancel()
            guestJob.cancel()
        }
    }
}
