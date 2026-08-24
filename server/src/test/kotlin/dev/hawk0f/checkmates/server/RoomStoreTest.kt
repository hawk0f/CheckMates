package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class RoomStoreTest {

    private fun newStore(): SqliteRoomStore {
        val file = File.createTempFile("rooms-${UUID.randomUUID()}", ".db")
        file.delete()
        file.deleteOnExit()
        return SqliteRoomStore(Db.init(file.absolutePath))
    }

    private fun snapshot(
        gameId: String,
        shortCode: String,
        lastActivityMillis: Long = System.currentTimeMillis()
    ) = RoomSnapshot(
        gameId = gameId,
        shortCode = shortCode,
        status = RoomStatus.IN_PROGRESS,
        timeControl = TimeControl(300, 3),
        uciHistory = listOf("e2e4", "e7e5", "g1f3"),
        players = listOf(
            RoomPlayerSnapshot(PieceColor.WHITE, "white-token", "Anna", 7L, 291_000),
            RoomPlayerSnapshot(PieceColor.BLACK, "black-token", "Boris", null, 298_500)
        ),
        turnStartedAtMillis = lastActivityMillis,
        lastActivityMillis = lastActivityMillis
    )

    @Test
    fun savedRoomIsRestoredWithBoardClocksAndTokens() = runTest {
        val store = newStore()
        store.save(snapshot("game-1", "ABCDEF"))

        val registry = RoomRegistry("http://localhost", null, store)
        assertEquals(1, registry.restoreFromStore())

        val room = assertNotNull(registry.byCode("ABCDEF"))
        val restored = room.snapshot()
        assertEquals(listOf("e2e4", "e7e5", "g1f3"), restored.uciHistory)
        assertEquals(RoomStatus.IN_PROGRESS, restored.status)
        assertEquals(TimeControl(300, 3), restored.timeControl)
        assertEquals(
            setOf("white-token" to 291_000L, "black-token" to 298_500L),
            restored.players.map { it.token to it.remainingMillis }.toSet()
        )
        assertEquals(PieceColor.WHITE, restored.players.first { it.token == "white-token" }.color)
        assertEquals(7L, restored.players.first { it.token == "white-token" }.userId)
    }

    @Test
    fun restoredRoomAcceptsTheStoredTokens() = runTest {
        val store = newStore()
        store.save(snapshot("game-2", "BCDEFG"))
        val registry = RoomRegistry("http://localhost", null, store)
        registry.restoreFromStore()

        val room = assertNotNull(registry.byId("game-2"))
        val tokens = room.snapshot().players.associate { it.token to it.color }
        assertNull(tokens["stranger-token"])
        assertEquals(PieceColor.BLACK, tokens["black-token"])
        assertEquals("BCDEFG", room.shortCode)
    }

    @Test
    fun deletedRoomIsNotRestored() = runTest {
        val store = newStore()
        store.save(snapshot("game-3", "CDEFGH"))
        store.delete("game-3")

        val registry = RoomRegistry("http://localhost", null, store)
        assertEquals(0, registry.restoreFromStore())
    }

    @Test
    fun roomsIdleBeyondTheWindowAreNotRestoredAndArePurged() = runTest {
        val store = newStore()
        val stale = System.currentTimeMillis() - SqliteRoomStore.RESUMABLE_WINDOW_MILLIS - 1000
        store.save(snapshot("game-4", "DEFGHI", lastActivityMillis = stale))

        val registry = RoomRegistry("http://localhost", null, store)
        assertEquals(0, registry.restoreFromStore())
        assertEquals(1, store.purgeExpired())
        assertEquals(0, store.purgeExpired())
    }
}
