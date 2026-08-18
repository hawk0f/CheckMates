package dev.hawk0f.chess.net

import dev.hawk0f.chess.shared.protocol.GameMessage
import dev.hawk0f.chess.shared.protocol.ProtocolJson
import dev.hawk0f.chess.shared.transport.GameTransport
import dev.hawk0f.chess.shared.transport.TransportConnectionState
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebSocketGameTransport(
    private val client: HttpClient,
    private val url: String,
    private val firstMessage: GameMessage? = null
) : GameTransport {

    private val _incoming = MutableSharedFlow<GameMessage>(extraBufferCapacity = 64)
    private val _connectionState = MutableStateFlow<TransportConnectionState>(TransportConnectionState.Connecting)
    private val sendQueue = Channel<GameMessage>(Channel.BUFFERED)
    private var job: Job? = null

    override val incoming: Flow<GameMessage> = _incoming
    override val connectionState: StateFlow<TransportConnectionState> = _connectionState.asStateFlow()

    fun start(scope: CoroutineScope) {
        if (job != null) {
            return
        }
        job = scope.launch {
            try {
                client.webSocket(url) {
                    _connectionState.value = TransportConnectionState.Connected
                    firstMessage?.let { send(Frame.Text(ProtocolJson.encode(it))) }
                    val sender = launch {
                        for (message in sendQueue) {
                            send(Frame.Text(ProtocolJson.encode(message)))
                        }
                    }
                    try {
                        for (frame in this.incoming) {
                            if (frame is Frame.Text) {
                                runCatching { ProtocolJson.decode(frame.readText()) }
                                    .getOrNull()
                                    ?.let { _incoming.emit(it) }
                            }
                        }
                    } finally {
                        sender.cancel()
                    }
                }
                _connectionState.value = TransportConnectionState.Closed(null)
            } catch (e: Exception) {
                _connectionState.value = TransportConnectionState.Closed(e.message)
            }
        }
    }

    override suspend fun send(message: GameMessage) {
        sendQueue.send(message)
    }

    override suspend fun close() {
        sendQueue.close()
        job?.cancel()
        _connectionState.value = TransportConnectionState.Closed(null)
    }
}
