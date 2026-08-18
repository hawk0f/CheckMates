package dev.hawk0f.chess.server

import dev.hawk0f.chess.shared.domain.GameOverReason
import dev.hawk0f.chess.shared.domain.PieceColor
import dev.hawk0f.chess.shared.protocol.GameMessage
import dev.hawk0f.chess.shared.protocol.ProtocolJson
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
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameServerTest {

    private fun testClient(builder: io.ktor.server.testing.ApplicationTestBuilder): HttpClient =
        builder.createClient {
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
    fun fullGameToCheckmateIsBroadcast() = testApplication {
        application { module() }
        val client = testClient(this)

        val created: CreateGameResponse = client.post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody(CreateGameRequest("Host"))
        }.body()

        val whiteMoves = listOf("e2e4", "f1c4", "d1h5", "h5f7")
        val blackMoves = listOf("e7e5", "b8c6", "g8f6")

        val hostDone = CompletableDeferred<GameMessage.GameOver>()
        val guestDone = CompletableDeferred<GameMessage.GameOver>()
        val guestJoined = CompletableDeferred<Unit>()

        coroutineScope {
        val hostJob = launch {
            client.webSocket("/ws/game/${created.gameId}?token=${created.playerToken}") {
                val colorMsg = awaitMessage { it is GameMessage.ColorAssigned } as GameMessage.ColorAssigned
                awaitMessage { it is GameMessage.OpponentJoined }
                val myMoves = if (colorMsg.color == PieceColor.WHITE) whiteMoves else blackMoves
                var sent = 0
                if (colorMsg.color == PieceColor.WHITE) {
                    sendMessage(GameMessage.MakeMove(myMoves[sent]))
                    sent++
                }
                while (true) {
                    val message = receiveMessage()
                    if (message is GameMessage.GameOver) {
                        hostDone.complete(message)
                        break
                    }
                    if (message is GameMessage.MoveApplied && sent < myMoves.size) {
                        val movesPlayed = message.moveNumber
                        val whiteToMove = movesPlayed % 2 == 0
                        val myTurn = (colorMsg.color == PieceColor.WHITE) == whiteToMove
                        if (myTurn) {
                            sendMessage(GameMessage.MakeMove(myMoves[sent]))
                            sent++
                        }
                    }
                }
            }
        }

        val guestJob = launch {
            client.webSocket("/ws/game/${created.gameId}") {
                sendMessage(GameMessage.JoinGame(created.shortCode, "Guest"))
                val colorMsg = awaitMessage { it is GameMessage.ColorAssigned } as GameMessage.ColorAssigned
                guestJoined.complete(Unit)
                val myMoves = if (colorMsg.color == PieceColor.WHITE) whiteMoves else blackMoves
                var sent = 0
                if (colorMsg.color == PieceColor.WHITE) {
                    sendMessage(GameMessage.MakeMove(myMoves[sent]))
                    sent++
                }
                while (true) {
                    val message = receiveMessage()
                    if (message is GameMessage.GameOver) {
                        guestDone.complete(message)
                        break
                    }
                    if (message is GameMessage.MoveApplied && sent < myMoves.size) {
                        val movesPlayed = message.moveNumber
                        val whiteToMove = movesPlayed % 2 == 0
                        val myTurn = (colorMsg.color == PieceColor.WHITE) == whiteToMove
                        if (myTurn) {
                            sendMessage(GameMessage.MakeMove(myMoves[sent]))
                            sent++
                        }
                    }
                }
            }
        }

        withTimeout(15_000) {
            val hostResult = hostDone.await()
            val guestResult = guestDone.await()
            assertEquals(GameOverReason.CHECKMATE, hostResult.reason)
            assertEquals(PieceColor.WHITE, hostResult.winner)
            assertEquals(hostResult, guestResult)
        }
        hostJob.join()
        guestJob.join()
        }
    }

    @Test
    fun illegalAndOutOfTurnMovesAreRejected() = testApplication {
        application { module() }
        val client = testClient(this)

        val created: CreateGameResponse = client.post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody(CreateGameRequest("Host"))
        }.body()

        client.webSocket("/ws/game/${created.gameId}?token=${created.playerToken}") {
            val color = (awaitMessage { it is GameMessage.ColorAssigned } as GameMessage.ColorAssigned).color

            sendMessage(GameMessage.MakeMove("e2e4"))
            val beforeJoin = awaitMessage { it is GameMessage.MoveRejected } as GameMessage.MoveRejected
            assertEquals("GAME_NOT_ACTIVE", beforeJoin.reason)

            coroutineScope {
            val guestSession = launch {
                client.webSocket("/ws/game/${created.gameId}") {
                    sendMessage(GameMessage.JoinGame(created.shortCode, "Guest"))
                    awaitMessage { it is GameMessage.GameOver }
                }
            }
            awaitMessage { it is GameMessage.OpponentJoined }

            if (color == PieceColor.WHITE) {
                sendMessage(GameMessage.MakeMove("e2e5"))
                val illegal = awaitMessage { it is GameMessage.MoveRejected } as GameMessage.MoveRejected
                assertEquals("ILLEGAL", illegal.reason)
            } else {
                sendMessage(GameMessage.MakeMove("e7e5"))
                val outOfTurn = awaitMessage { it is GameMessage.MoveRejected } as GameMessage.MoveRejected
                assertEquals("NOT_YOUR_TURN", outOfTurn.reason)
            }

            sendMessage(GameMessage.Resign)
            val over = awaitMessage { it is GameMessage.GameOver } as GameMessage.GameOver
            assertEquals(GameOverReason.RESIGNATION, over.reason)
            assertEquals(color.opposite, over.winner)
            guestSession.join()
            }
        }
    }

    @Test
    fun reconnectRestoresStateViaResync() = testApplication {
        application { module() }
        val client = testClient(this)

        val created: CreateGameResponse = client.post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody(CreateGameRequest("Host"))
        }.body()

        var guestToken: String? = null
        client.webSocket("/ws/game/${created.gameId}") {
            sendMessage(GameMessage.JoinGame(created.shortCode, "Guest"))
            val credentials = awaitMessage { it is GameMessage.GameCreated } as GameMessage.GameCreated
            guestToken = credentials.playerToken
        }

        client.webSocket("/ws/game/${created.gameId}") {
            sendMessage(GameMessage.Reconnect(created.gameId, guestToken!!))
            val resync = awaitMessage { it is GameMessage.Resync } as GameMessage.Resync
            assertTrue(resync.uciHistory.isEmpty())
        }

        client.webSocket("/ws/game/${created.gameId}") {
            sendMessage(GameMessage.Reconnect(created.gameId, "wrong-token"))
            val error = awaitMessage { it is GameMessage.ProtocolError } as GameMessage.ProtocolError
            assertEquals("BAD_TOKEN", error.code)
        }
    }

    @Test
    fun unknownGameReturnsProtocolError() = testApplication {
        application { module() }
        val client = testClient(this)
        client.webSocket("/ws/game/nope") {
            val error = receiveMessage()
            assertIs<GameMessage.ProtocolError>(error)
            assertEquals("GAME_NOT_FOUND", error.code)
        }
    }
}
