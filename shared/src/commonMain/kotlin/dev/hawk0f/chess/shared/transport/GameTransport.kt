package dev.hawk0f.chess.shared.transport

import dev.hawk0f.chess.shared.protocol.GameMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface TransportConnectionState {
    data object Connecting : TransportConnectionState
    data object Connected : TransportConnectionState
    data object Reconnecting : TransportConnectionState
    data class Closed(val reason: String?) : TransportConnectionState
}

interface GameTransport {
    val incoming: Flow<GameMessage>
    val connectionState: StateFlow<TransportConnectionState>
    suspend fun send(message: GameMessage)
    suspend fun close()
}
