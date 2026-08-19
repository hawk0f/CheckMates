package dev.hawk0f.checkmates.net

import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.protocol.ProtocolJson
import dev.hawk0f.checkmates.shared.transport.GameTransport
import dev.hawk0f.checkmates.shared.transport.TransportConnectionState
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebSocketGameTransport(
    private val client: HttpClient,
    private val url: String,
    private val firstMessage: GameMessage? = null,
    gameId: String? = null,
    playerToken: String? = null
) : GameTransport {

    private val _incoming = MutableSharedFlow<GameMessage>(extraBufferCapacity = 64)
    private val _connectionState = MutableStateFlow<TransportConnectionState>(TransportConnectionState.Connecting)
    private val sendQueue = Channel<GameMessage>(Channel.BUFFERED)
    private var job: Job? = null
    private var closedByUser = false
    private var credentials: Pair<String, String>? =
        if (gameId != null && playerToken != null) gameId to playerToken else null

    override val incoming: Flow<GameMessage> = _incoming
    override val connectionState: StateFlow<TransportConnectionState> = _connectionState.asStateFlow()

    fun start(scope: CoroutineScope) {
        if (job != null) {
            return
        }
        job = scope.launch {
            var attempt = 0
            var everConnected = false
            while (!closedByUser) {
                try {
                    client.webSocket(url) {
                        attempt = 0
                        _connectionState.value = TransportConnectionState.Connected
                        val opening = if (everConnected && credentials != null) {
                            GameMessage.Reconnect(credentials!!.first, credentials!!.second)
                        } else {
                            firstMessage
                        }
                        everConnected = true
                        opening?.let { send(Frame.Text(ProtocolJson.encode(it))) }
                        val sender = launch {
                            for (message in sendQueue) {
                                send(Frame.Text(ProtocolJson.encode(message)))
                            }
                        }
                        try {
                            for (frame in this.incoming) {
                                if (frame is Frame.Text) {
                                    val message = runCatching { ProtocolJson.decode(frame.readText()) }.getOrNull()
                                    if (message != null) {
                                        if (message is GameMessage.GameCreated && message.playerToken.isNotEmpty()) {
                                            credentials = message.gameId to message.playerToken
                                        }
                                        _incoming.emit(message)
                                    }
                                }
                            }
                        } finally {
                            sender.cancel()
                        }
                    }
                } catch (_: Exception) {
                }
                if (closedByUser) {
                    break
                }
                if (credentials == null && everConnected) {
                    break
                }
                _connectionState.value = TransportConnectionState.Reconnecting
                attempt++
                if (attempt > 8) {
                    break
                }
                delay(minOf(1000L shl (attempt - 1), 15_000L))
            }
            if (!closedByUser) {
                _connectionState.value = TransportConnectionState.Closed("connection lost")
            }
        }
    }

    override suspend fun send(message: GameMessage) {
        sendQueue.send(message)
    }

    override suspend fun close() {
        closedByUser = true
        sendQueue.close()
        job?.cancel()
        _connectionState.value = TransportConnectionState.Closed(null)
    }
}
