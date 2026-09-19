package dev.hawk0f.checkmates.session

import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalGameHistoryStoreTest {

    @Test
    fun serverCopyReplacesMatchingLocalGame() {
        val local = game(id = -1, finishedAtMillis = 10_000)
        val server = game(id = 42, finishedAtMillis = 10_500)

        val merged = mergeGameHistory(listOf(server), listOf(local))

        assertEquals(listOf(42L), merged.map { it.id })
    }

    @Test
    fun repeatedGamesAtDifferentTimesArePreserved() {
        val first = game(id = -1, finishedAtMillis = 10_000)
        val later = game(id = -2, finishedAtMillis = 80_000)

        val merged = mergeGameHistory(emptyList(), listOf(first, later))

        assertEquals(listOf(-2L, -1L), merged.map { it.id })
    }

    private fun game(id: Long, finishedAtMillis: Long) = GameHistoryItem(
        id = id,
        mode = "hotseat",
        myColor = null,
        whiteName = "White",
        blackName = "Black",
        winner = PieceColor.WHITE,
        reason = GameOverReason.CHECKMATE,
        uciHistory = listOf("f2f3", "e7e5", "g2g4", "d8h4"),
        finishedAtMillis = finishedAtMillis
    )
}
