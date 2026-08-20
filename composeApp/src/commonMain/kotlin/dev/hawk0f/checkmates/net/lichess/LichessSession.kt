package dev.hawk0f.checkmates.net.lichess

import dev.hawk0f.checkmates.session.ActiveGameSession
import dev.hawk0f.checkmates.session.GameSessionHolder

object LichessSessionStarter {

    fun open(gameId: String): Boolean {
        val token = LichessAuth.token ?: return false
        val username = LichessAuth.username.value ?: return false
        val transport = LichessGameTransport(
            api = LichessAuth.api,
            token = token,
            gameId = gameId,
            myUsername = username
        )
        val session = ActiveGameSession(transport, kind = "lichess", myName = username)
        GameSessionHolder.install(session)
        transport.start(session.scope)
        return true
    }

    fun currentGameId(): String? =
        (GameSessionHolder.current?.transport as? LichessGameTransport)?.gameId
}
