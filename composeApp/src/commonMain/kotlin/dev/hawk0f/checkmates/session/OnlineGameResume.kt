package dev.hawk0f.checkmates.session

import dev.hawk0f.checkmates.net.ServerConfig
import dev.hawk0f.checkmates.net.WebSocketGameTransport
import dev.hawk0f.checkmates.net.configuredWebSocketClient
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

object OnlineGameResume {

    private const val ATTACH_TIMEOUT_MILLIS = 12_000L

    fun stored(): SavedOnlineGame? = OnlineGameStore.load()

    suspend fun resume(): Boolean {
        if (GameSessionHolder.current != null) {
            return true
        }
        val saved = OnlineGameStore.load() ?: return false
        val transport = WebSocketGameTransport(
            client = configuredWebSocketClient(),
            url = ServerConfig.wsGameUrl(saved.gameId, saved.playerToken),
            gameId = saved.gameId,
            playerToken = saved.playerToken
        )
        val session = ActiveGameSession(
            transport = transport,
            kind = "online",
            myName = saved.myName,
            gameId = saved.gameId,
            playerToken = saved.playerToken
        )
        GameSessionHolder.install(session)
        transport.start(session.scope)
        val attached = withTimeoutOrNull(ATTACH_TIMEOUT_MILLIS) {
            session.myColor.filterNotNull().first()
        }
        if (attached == null) {
            GameSessionHolder.detach()
            return false
        }
        return true
    }
}
