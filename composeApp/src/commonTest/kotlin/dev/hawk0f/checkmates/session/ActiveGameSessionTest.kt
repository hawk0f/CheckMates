package dev.hawk0f.checkmates.session

import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.transport.GameTransport
import dev.hawk0f.checkmates.shared.transport.TransportConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ActiveGameSessionTest {

    private class FakeStore : OnlineGamePersistence {
        var stored: SavedOnlineGame? = null
        var cleared = 0

        override fun load(): SavedOnlineGame? = stored

        override fun save(game: SavedOnlineGame) {
            stored = game
        }

        override fun clear() {
            cleared++
            stored = null
        }
    }

    private class FakeTransport : GameTransport {
        val messages = MutableSharedFlow<GameMessage>(extraBufferCapacity = 16)
        override val incoming = messages
        override val connectionState = MutableStateFlow<TransportConnectionState>(TransportConnectionState.Connected)

        override suspend fun send(message: GameMessage) = Unit

        override suspend fun close() = Unit
    }

    @Test
    fun anOnlineHostSessionStoresItsResumeCredentials() {
        val store = FakeStore()
        ActiveGameSession(
            transport = FakeTransport(),
            kind = "online",
            myName = "Pixel",
            gameId = "game-1",
            playerToken = "token-1",
            resumeStore = store
        )

        val saved = assertNotNull(store.stored)
        assertEquals("game-1", saved.gameId)
        assertEquals("token-1", saved.playerToken)
        assertEquals("Pixel", saved.myName)
    }

    @Test
    fun aGuestSessionStoresTheCredentialsTheServerSends() = runTest {
        val store = FakeStore()
        val transport = FakeTransport()
        ActiveGameSession(transport = transport, kind = "online", myName = "Fold", resumeStore = store)

        assertNull(store.stored)
        withContext(Dispatchers.Default) {
            withTimeout(TIMEOUT_MILLIS) {
                transport.messages.emit(
                    GameMessage.GameCreated(
                        gameId = "game-2",
                        shortCode = "ABC234",
                        joinUrl = "",
                        playerToken = "token-2"
                    )
                )
                while (store.stored == null) {
                    yield()
                }
            }
        }

        val saved = assertNotNull(store.stored)
        assertEquals("game-2", saved.gameId)
        assertEquals("token-2", saved.playerToken)
    }

    @Test
    fun aNearbySessionKeepsNoResumePoint() {
        val store = FakeStore()
        ActiveGameSession(
            transport = FakeTransport(),
            kind = "ble",
            myName = "Fold",
            gameId = "game-3",
            playerToken = "token-3",
            resumeStore = store
        )

        assertNull(store.stored)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
