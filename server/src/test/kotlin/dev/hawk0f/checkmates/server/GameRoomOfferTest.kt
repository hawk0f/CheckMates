package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class GameRoomOfferTest {

    private suspend fun startedRoom(): GameRoom {
        val room = GameRoom(
            gameId = "game-1",
            shortCode = "ABCDEF",
            hostToken = "white-token",
            hostName = "Host",
            hostColor = PieceColor.WHITE
        )
        room.seatGuest("black-token", "Guest")
        return room
    }

    @Test
    fun aTakebackOfferedBeforeTheEndCannotUndoAFinishedGame() = runTest {
        val room = startedRoom()
        room.handle("white-token", GameMessage.MakeMove("e2e4"))
        room.handle("black-token", GameMessage.MakeMove("e7e5"))
        room.handle("white-token", GameMessage.OfferTakeback)
        room.handle("white-token", GameMessage.Resign)
        assertEquals(RoomStatus.FINISHED, room.status)

        val historyBefore = room.snapshot().uciHistory
        room.handle("black-token", GameMessage.AcceptTakeback)

        assertEquals(historyBefore, room.snapshot().uciHistory)
        assertEquals(RoomStatus.FINISHED, room.status)
    }

    @Test
    fun aReconnectMessageIsAnsweredWithAResync() = runTest {
        val room = startedRoom()
        room.handle("white-token", GameMessage.MakeMove("e2e4"))

        room.handle("white-token", GameMessage.Reconnect("game-1", "white-token"))

        assertEquals(listOf("e2e4"), room.snapshot().uciHistory)
        assertEquals(RoomStatus.IN_PROGRESS, room.status)
    }
}
