package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ResyncResultTest {

    private fun newRoom() = GameRoom(
        gameId = "game",
        shortCode = "ABC234",
        hostToken = "host",
        hostName = "Host",
        hostColor = PieceColor.WHITE
    )

    @Test
    fun aLiveRoomResyncsWithoutAResult() = runTest {
        val room = newRoom()
        val resync = room.resyncMessage()
        assertNull(resync.resultReason)
        assertNull(resync.resultWinner)
    }

    @Test
    fun aFinishedRoomCarriesTheResultInEveryResync() = runTest {
        val room = newRoom()
        room.seatGuest("guest", "Guest")
        room.handle("host", GameMessage.Resign)

        val resync = room.resyncMessage()
        assertEquals(GameOverReason.RESIGNATION, resync.resultReason)
        assertEquals(PieceColor.BLACK, resync.resultWinner)
    }
}
