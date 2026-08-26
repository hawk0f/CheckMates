package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PgnBuilder
import dev.hawk0f.checkmates.shared.domain.PgnReader
import dev.hawk0f.checkmates.shared.domain.PieceColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PgnCustomStartTest {

    private val promotionFen = "8/P6k/8/8/8/8/6p1/6K1 w - - 0 1"

    @Test
    fun aGameFromACustomPositionExportsSanAndTheSetUpTags() {
        val pgn = PgnBuilder.build(
            whiteName = "White",
            blackName = "Black",
            winner = PieceColor.WHITE,
            reason = GameOverReason.RESIGNATION,
            uciHistory = listOf("a7a8n"),
            startFen = promotionFen
        )

        assertTrue(pgn.contains("[SetUp \"1\"]"), pgn)
        assertTrue(pgn.contains("[FEN \"$promotionFen\"]"), pgn)
        assertTrue(pgn.contains("a8=N"), pgn)
    }

    @Test
    fun aCustomGameExportedWithoutItsFenFallsBackInsteadOfThrowing() {
        val pgn = PgnBuilder.build(
            whiteName = "White",
            blackName = "Black",
            winner = null,
            reason = null,
            uciHistory = listOf("a7a8q")
        )

        assertTrue(pgn.contains("a7a8q"), pgn)
    }

    @Test
    fun anUnfinishedGameWithVariationsIsImported() {
        val pgn = """
            [Event "Test"]
            [Result "*"]

            1. e4 e5 2. Nf3 (2. f4 exf4) 2... Nc6 ${'$'}1 *
        """.trimIndent()

        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6"), PgnReader.uciMoves(pgn))
    }
}
