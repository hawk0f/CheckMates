package dev.hawk0f.chess.shared

import dev.hawk0f.chess.shared.domain.GameOverReason
import dev.hawk0f.chess.shared.domain.PgnBuilder
import dev.hawk0f.chess.shared.domain.PieceColor
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PgnBuilderTest {

    @Test
    fun scholarsMateProducesSanWithHeaders() {
        val pgn = PgnBuilder.build(
            whiteName = "Alice",
            blackName = "Bob",
            winner = PieceColor.WHITE,
            reason = GameOverReason.CHECKMATE,
            uciHistory = listOf("e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7"),
            dateMillis = 1_774_224_000_000
        )
        assertContains(pgn, "[White \"Alice\"]")
        assertContains(pgn, "[Black \"Bob\"]")
        assertContains(pgn, "[Result \"1-0\"]")
        assertContains(pgn, "[Date \"2026.03.23\"]")
        assertContains(pgn, "Qxf7#")
        assertTrue(pgn.trimEnd().endsWith("1-0"))
    }

    @Test
    fun drawAndCastlingAndPromotionRender() {
        val pgn = PgnBuilder.build(
            whiteName = "W",
            blackName = "B",
            winner = null,
            reason = GameOverReason.DRAW_AGREED,
            uciHistory = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6", "e1g1"),
            dateMillis = null
        )
        assertContains(pgn, "[Result \"1/2-1/2\"]")
        assertContains(pgn, "[Date \"????.??.??\"]")
        assertContains(pgn, "O-O")
    }

    @Test
    fun quotesInNamesAreEscaped() {
        val pgn = PgnBuilder.build(
            whiteName = "An \"na\"",
            blackName = "B",
            winner = PieceColor.BLACK,
            reason = GameOverReason.RESIGNATION,
            uciHistory = emptyList(),
            dateMillis = null
        )
        assertContains(pgn, "[White \"An \\\"na\\\"\"]")
        assertContains(pgn, "[Result \"0-1\"]")
        assertEquals("0-1", pgn.trim().lines().last())
    }
}
