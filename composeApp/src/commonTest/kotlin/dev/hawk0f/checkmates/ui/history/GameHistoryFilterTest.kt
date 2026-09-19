package dev.hawk0f.checkmates.ui.history

import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import kotlin.test.Test
import kotlin.test.assertEquals

class GameHistoryFilterTest {

    private val games = listOf(
        game(1, "online", "Alice", "Bob", PieceColor.WHITE, PieceColor.WHITE),
        game(2, "ble", "Carol", "Alice", PieceColor.BLACK, PieceColor.WHITE),
        game(3, "computer", "Alice", "Computer", PieceColor.WHITE, null),
        game(4, "hotseat", "White", "Black", null, PieceColor.BLACK)
    )

    @Test
    fun searchMatchesEitherPlayerIgnoringCase() {
        val filtered = filterGameHistory(
            games,
            query = "ali",
            mode = HistoryModeFilter.ALL,
            result = HistoryResultFilter.ALL
        )

        assertEquals(listOf(1L, 2L, 3L), filtered.map { it.id })
    }

    @Test
    fun modeAndResultFiltersCompose() {
        val filtered = filterGameHistory(
            games,
            query = "",
            mode = HistoryModeFilter.REMOTE,
            result = HistoryResultFilter.WINS
        )

        assertEquals(listOf(1L), filtered.map { it.id })
    }

    @Test
    fun sharedBoardGamesDoNotPretendToBePersonalWinsOrLosses() {
        val wins = filterGameHistory(games, "", HistoryModeFilter.ALL, HistoryResultFilter.WINS)
        val losses = filterGameHistory(games, "", HistoryModeFilter.ALL, HistoryResultFilter.LOSSES)

        assertEquals(listOf(1L), wins.map { it.id })
        assertEquals(listOf(2L), losses.map { it.id })
    }

    @Test
    fun statisticsUseOnlyPerspectiveGamesForWinRate() {
        val stats = calculateGameHistoryStats(games)

        assertEquals(4, stats.totalGames)
        assertEquals(3, stats.personalGames)
        assertEquals(1, stats.wins)
        assertEquals(1, stats.draws)
        assertEquals(1, stats.losses)
        assertEquals(33, stats.winRate)
        assertEquals(1, stats.averageMoves)
    }

    @Test
    fun statisticsFindTheMostFrequentOpening() {
        val ruyLopez = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5")
        val scandinavian = listOf("e2e4", "d7d5", "e4d5", "d8d5")
        val openingGames = listOf(
            game(10, "online", "Alice", "Bob", PieceColor.WHITE, PieceColor.WHITE, ruyLopez),
            game(11, "computer", "Alice", "Computer", PieceColor.WHITE, PieceColor.BLACK, ruyLopez),
            game(12, "online", "Alice", "Carol", PieceColor.WHITE, PieceColor.WHITE, scandinavian)
        )

        val stats = calculateGameHistoryStats(openingGames)

        assertEquals("Ruy Lopez", stats.topOpening)
        assertEquals(3, stats.averageMoves)
    }

    private fun game(
        id: Long,
        mode: String,
        whiteName: String,
        blackName: String,
        myColor: PieceColor?,
        winner: PieceColor?,
        history: List<String> = listOf("e2e4")
    ) = GameHistoryItem(
        id = id,
        mode = mode,
        myColor = myColor,
        whiteName = whiteName,
        blackName = blackName,
        winner = winner,
        reason = if (winner == null) GameOverReason.STALEMATE else GameOverReason.RESIGNATION,
        uciHistory = history,
        finishedAtMillis = id
    )
}
