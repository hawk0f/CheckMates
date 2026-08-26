package dev.hawk0f.checkmates.session

import dev.hawk0f.checkmates.platform.epochMillis
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.transport.GameTransport
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
    val myName: String = "Me",
    gameId: String? = null,
    playerToken: String? = null,
    private val resumeStore: OnlineGamePersistence = OnlineGameStore
) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val myColor = MutableStateFlow<PieceColor?>(null)
    val opponentName = MutableStateFlow<String?>(null)
    val messages = MutableSharedFlow<GameMessage>(extraBufferCapacity = 64)

    private var credentials: Pair<String, String>? = null

    init {
        rememberCredentials(gameId, playerToken)
        scope.launch {
            transport.incoming.collect { message ->
                when (message) {
                    is GameMessage.ColorAssigned -> myColor.value = message.color
                    is GameMessage.RematchStarted -> myColor.value = message.color
                    is GameMessage.OpponentJoined -> {
                        opponentName.value = message.opponentName
                        rememberResumePoint()
                    }

                    is GameMessage.GameCreated -> rememberCredentials(message.gameId, message.playerToken)
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

    private fun rememberCredentials(gameId: String?, playerToken: String?) {
        if (kind != "online" || gameId.isNullOrBlank() || playerToken.isNullOrBlank()) {
            return
        }
        credentials = gameId to playerToken
        rememberResumePoint()
    }

    private fun rememberResumePoint() {
        val (gameId, playerToken) = credentials ?: return
        resumeStore.save(
            SavedOnlineGame(
                gameId = gameId,
                playerToken = playerToken,
                myName = myName,
                opponentName = opponentName.value,
                savedAtMillis = epochMillis()
            )
        )
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
        OnlineGameStore.clear()
    }

    fun detach() {
        current?.shutdown()
        current = null
    }
}
