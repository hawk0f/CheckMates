package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.protocol.ProtocolJson
import dev.hawk0f.checkmates.shared.protocol.ShortCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtocolTest {

    private val samples = listOf(
        GameMessage.CreateGame("Misha"),
        GameMessage.JoinGame("AB23CD", "Друг"),
        GameMessage.MakeMove("e2e4"),
        GameMessage.OfferDraw,
        GameMessage.AcceptDraw,
        GameMessage.DeclineDraw,
        GameMessage.Resign,
        GameMessage.RequestResync,
        GameMessage.Reconnect("game-1", "token-1"),
        GameMessage.Ping,
        GameMessage.GameCreated("game-1", "AB23CD", "https://example.com/game/AB23CD", "token-1"),
        GameMessage.ColorAssigned(PieceColor.BLACK),
        GameMessage.OpponentJoined("Друг"),
        GameMessage.MoveApplied("e7e8q", "fen here", 12),
        GameMessage.MoveRejected("e2e5", "ILLEGAL"),
        GameMessage.DrawOffered,
        GameMessage.DrawDeclined,
        GameMessage.GameOver(GameOverReason.CHECKMATE, PieceColor.WHITE),
        GameMessage.Resync("fen", listOf("e2e4", "e7e5"), true),
        GameMessage.OpponentConnectionChanged(false),
        GameMessage.Pong,
        GameMessage.ProtocolError("GAME_NOT_FOUND", "game expired")
    )

    @Test
    fun jsonRoundTripForEveryMessage() {
        for (message in samples) {
            assertEquals(message, ProtocolJson.decode(ProtocolJson.encode(message)), "round trip failed for $message")
        }
    }

    @Test
    fun shortCodeGeneratesValidCodes() {
        repeat(100) {
            val code = ShortCode.generate()
            assertTrue(ShortCode.isValid(code), "invalid code generated: $code")
        }
    }

    @Test
    fun shortCodeRejectsAmbiguousCharacters() {
        assertFalse(ShortCode.isValid("00OO11"))
        assertFalse(ShortCode.isValid("ABC"))
        assertFalse(ShortCode.isValid("abc234"))
        assertTrue(ShortCode.isValid("AB23CD"))
    }

    @Test
    fun extractFromTextFindsCodeInUrlAndRawInput() {
        assertEquals("AB23CD", ShortCode.extractFromText("https://chess.example.com/game/AB23CD"))
        assertEquals("AB23CD", ShortCode.extractFromText(" ab23cd "))
        assertNull(ShortCode.extractFromText("https://chess.example.com/about"))
    }
}
