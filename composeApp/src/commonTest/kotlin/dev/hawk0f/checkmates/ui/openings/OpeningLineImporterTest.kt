package dev.hawk0f.checkmates.ui.openings

import dev.hawk0f.checkmates.shared.domain.PieceColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpeningLineImporterTest {

    @Test
    fun importsThePgnMainLine() {
        val pgn = """
            [Event "Training"]

            1. e4 e5 2. Nf3 Nc6 3. Bb5 a6
        """.trimIndent()

        val line = OpeningLineImporter.import("Ruy Lopez", pgn, PieceColor.WHITE)

        assertEquals("Ruy Lopez", line?.name)
        assertEquals(PieceColor.WHITE, line?.trainedColor)
        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6"), line?.moves)
    }

    @Test
    fun rejectsAnInvalidOrUnnamedLine() {
        assertNull(OpeningLineImporter.import("", "1. e4 e5", PieceColor.WHITE))
        assertNull(OpeningLineImporter.import("Broken", "not chess", PieceColor.BLACK))
    }
}
