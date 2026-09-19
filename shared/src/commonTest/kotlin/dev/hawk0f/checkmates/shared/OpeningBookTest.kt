package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.opening.OpeningBook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpeningBookTest {

    @Test
    fun ambiguousOpeningIsNotNamedTooEarly() {
        assertNull(OpeningBook.identify(listOf("e2e4", "e7e5", "g1f3", "b8c6")))
    }

    @Test
    fun openingIsIdentifiedWhenTheLineBecomesUnique() {
        val opening = OpeningBook.identify(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5"))

        assertEquals("Ruy Lopez", opening?.name)
    }

    @Test
    fun openingNameSurvivesLaterMovesOutsideTheBook() {
        val opening = OpeningBook.identify(
            listOf("e2e4", "d7d5", "e4d5", "d8d5", "b1c3", "d5a5", "g1f3")
        )

        assertEquals("Scandinavian Defence", opening?.name)
    }
}
