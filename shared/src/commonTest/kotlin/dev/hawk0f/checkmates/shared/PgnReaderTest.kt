package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.domain.PgnReader
import kotlin.test.Test
import kotlin.test.assertEquals

class PgnReaderTest {

    @Test
    fun readsPlainSanMoves() {
        val moves = PgnReader.uciMoves("1. e4 e5 2. Nf3 Nc6 3. Bb5")
        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5"), moves)
    }

    @Test
    fun ignoresHeadersCommentsAndResult() {
        val pgn = """
            [Event "Test"]
            [White "A"]

            1. d4 d5 {solid} 2. c4! e6 1/2-1/2
        """.trimIndent()
        assertEquals(listOf("d2d4", "d7d5", "c2c4", "e7e6"), PgnReader.uciMoves(pgn))
    }

    @Test
    fun returnsEmptyForGarbage() {
        assertEquals(emptyList(), PgnReader.uciMoves(""))
    }
}
