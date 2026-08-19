package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.BleCodec
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BleCodecTest {

    @Test
    fun guestToHostRoundTrip() {
        val messages = listOf(
            GameMessage.MakeMove("e2e4"),
            GameMessage.MakeMove("e7e8q"),
            GameMessage.JoinGame("", "Гость"),
            GameMessage.OfferDraw,
            GameMessage.AcceptDraw,
            GameMessage.DeclineDraw,
            GameMessage.Resign,
            GameMessage.RequestResync
        )
        for (message in messages) {
            val bytes = BleCodec.encodeToHost(message)!!
            assertTrue(bytes.size <= BleCodec.MAX_MESSAGE_BYTES)
            assertEquals(message, BleCodec.decodeFromGuest(bytes), "round trip failed for $message")
        }
    }

    @Test
    fun hostToGuestRoundTrip() {
        val messages = listOf(
            GameMessage.MoveApplied("e2e4", "", 0),
            GameMessage.ColorAssigned(PieceColor.WHITE),
            GameMessage.ColorAssigned(PieceColor.BLACK),
            GameMessage.OpponentJoined("Host"),
            GameMessage.MoveRejected("e2e5", "ILLEGAL"),
            GameMessage.DrawOffered,
            GameMessage.DrawDeclined,
            GameMessage.GameOver(GameOverReason.CHECKMATE, PieceColor.WHITE),
            GameMessage.GameOver(GameOverReason.STALEMATE, null),
            GameMessage.GameOver(GameOverReason.DRAW_AGREED, null),
            GameMessage.GameOver(GameOverReason.RESIGNATION, PieceColor.BLACK)
        )
        for (message in messages) {
            val bytes = BleCodec.encodeToGuest(message)!!
            assertTrue(bytes.size <= BleCodec.MAX_MESSAGE_BYTES)
            assertEquals(message, BleCodec.decodeFromHost(bytes), "round trip failed for $message")
        }
    }

    @Test
    fun unsupportedMessagesEncodeToNull() {
        assertNull(BleCodec.encodeToHost(GameMessage.Ping))
        assertNull(BleCodec.encodeToGuest(GameMessage.Pong))
        assertNull(BleCodec.encodeToGuest(GameMessage.Resync("fen", emptyList(), false)))
    }

    @Test
    fun longCyrillicNameFitsInMtu() {
        val bytes = BleCodec.encodeToHost(GameMessage.JoinGame("", "Владимир"))!!
        assertTrue(bytes.size <= BleCodec.MAX_MESSAGE_BYTES)
    }

    @Test
    fun garbageDecodesToNull() {
        assertNull(BleCodec.decodeFromGuest(byteArrayOf()))
        assertNull(BleCodec.decodeFromHost("Zjunk".encodeToByteArray()))
    }
}
