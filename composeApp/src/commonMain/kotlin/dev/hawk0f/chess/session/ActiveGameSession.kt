package dev.hawk0f.chess.session

import dev.hawk0f.chess.shared.domain.PieceColor
import dev.hawk0f.chess.shared.protocol.GameMessage
import dev.hawk0f.chess.shared.transport.GameTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ActiveGameSession(
    val transport: GameTransport,
    val kind: String = "online",
    val myName: String = "Me"
) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val myColor = MutableStateFlow<PieceColor?>(null)
    val opponentName = MutableStateFlow<String?>(null)
    val messages = MutableSharedFlow<GameMessage>(extraBufferCapacity = 64)

    init {
        scope.launch {
            transport.incoming.collect { message ->
                when (message) {
                    is GameMessage.ColorAssigned -> myColor.value = message.color
                    is GameMessage.OpponentJoined -> opponentName.value = message.opponentName
                    else -> {}
                }
                messages.emit(message)
            }
        }
    }

    suspend fun send(message: GameMessage) {
        transport.send(message)
    }

    fun shutdown() {
        scope.launch {
            transport.close()
            scope.cancel()
        }
    }
}

object GameSessionHolder {

    var current: ActiveGameSession? = null
        private set

    fun install(session: ActiveGameSession) {
        current?.shutdown()
        current = session
    }

    fun clear() {
        current?.shutdown()
        current = null
    }
}
