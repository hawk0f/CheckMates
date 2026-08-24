package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.GameSpeed
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RatingTest {

    private fun newRepository(): Pair<RatingRepository, UserRepository> {
        val file = File.createTempFile("ratings-${UUID.randomUUID()}", ".db")
        file.delete()
        file.deleteOnExit()
        val database = Db.init(file.absolutePath)
        return RatingRepository(database) to UserRepository(database)
    }

    private suspend fun newUser(users: UserRepository, login: String): Long {
        val result = users.register(login, "password123", login)
        return (result as AuthResult.Success).profile.id
    }

    @Test
    fun winnerGainsAndLoserDropsBySameOrderOfMagnitude() {
        val winner = Glicko2.update(Glicko2Rating(1500.0, 200.0, 0.06), Glicko2Rating(1500.0, 200.0, 0.06), 1.0)
        val loser = Glicko2.update(Glicko2Rating(1500.0, 200.0, 0.06), Glicko2Rating(1500.0, 200.0, 0.06), 0.0)
        assertTrue(winner.rating > 1500.0, "winner rating was ${winner.rating}")
        assertTrue(loser.rating < 1500.0, "loser rating was ${loser.rating}")
        assertEquals((winner.rating - 1500.0).roundToInt(), (1500.0 - loser.rating).roundToInt())
    }

    @Test
    fun deviationShrinksAfterAGameAndGrowsWhileIdle() {
        val played = Glicko2.update(Glicko2Rating(), Glicko2Rating(), 1.0)
        assertTrue(played.deviation < Glicko2.DEFAULT_DEVIATION, "deviation was ${played.deviation}")
        val idle = Glicko2.decayed(played, 400L * 24 * 60 * 60 * 1000)
        assertTrue(idle.deviation > played.deviation, "idle deviation was ${idle.deviation}")
        assertTrue(idle.deviation <= Glicko2.MAX_DEVIATION)
    }

    @Test
    fun beatingAStrongerOpponentGainsMoreThanBeatingAWeakerOne() {
        val versusStronger = Glicko2.update(Glicko2Rating(1500.0, 80.0, 0.06), Glicko2Rating(1900.0, 80.0, 0.06), 1.0)
        val versusWeaker = Glicko2.update(Glicko2Rating(1500.0, 80.0, 0.06), Glicko2Rating(1100.0, 80.0, 0.06), 1.0)
        assertTrue(
            versusStronger.rating - 1500.0 > versusWeaker.rating - 1500.0,
            "stronger=${versusStronger.rating} weaker=${versusWeaker.rating}"
        )
    }

    @Test
    fun appliedResultIsPersistedPerSpeed() = runTest {
        val (ratings, users) = newRepository()
        val white = newUser(users, "whiteplayer")
        val black = newUser(users, "blackplayer")

        val changes = ratings.applyResult(GameSpeed.BLITZ, white, black, 1.0)
        assertEquals(2, changes.size)
        assertTrue(changes.first { it.userId == white }.after > 1500)
        assertTrue(changes.first { it.userId == black }.after < 1500)

        val blitz = ratings.ratingsOf(white).first { it.speed == GameSpeed.BLITZ }
        assertEquals(1, blitz.games)
        assertTrue(blitz.provisional)
        val rapid = ratings.ratingsOf(white).first { it.speed == GameSpeed.RAPID }
        assertEquals(0, rapid.games)
        assertEquals(1500, rapid.rating)
    }

    @Test
    fun leaderboardOnlyListsPlayersPastTheProvisionalGameCount() = runTest {
        val (ratings, users) = newRepository()
        val white = newUser(users, "steadyplayer")
        val black = newUser(users, "rivalplayer")

        repeat(RatingRepository.MIN_RANKED_GAMES - 1) {
            ratings.applyResult(GameSpeed.RAPID, white, black, 1.0)
        }
        assertTrue(ratings.leaderboard(GameSpeed.RAPID).isEmpty())

        ratings.applyResult(GameSpeed.RAPID, white, black, 1.0)
        val board = ratings.leaderboard(GameSpeed.RAPID)
        assertEquals(2, board.size)
        assertEquals(white, board.first().userId)
        assertTrue(board.first().rating > board.last().rating)
    }

    @Test
    fun speedIsDerivedFromTheEstimatedGameLength() {
        assertEquals(GameSpeed.BULLET, GameSpeed.of(TimeControl(60, 0)))
        assertEquals(GameSpeed.BLITZ, GameSpeed.of(TimeControl(180, 2)))
        assertEquals(GameSpeed.RAPID, GameSpeed.of(TimeControl(600, 0)))
        assertEquals(GameSpeed.CLASSICAL, GameSpeed.of(TimeControl(1800, 30)))
        assertEquals(GameSpeed.CLASSICAL, GameSpeed.of(null))
    }
}
