package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.GameSpeed
import dev.hawk0f.checkmates.shared.protocol.LeaderboardEntry
import dev.hawk0f.checkmates.shared.protocol.RatingEntry
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

data class RatingChange(
    val userId: Long,
    val speed: GameSpeed,
    val before: Int,
    val after: Int
)

class RatingRepository(private val database: Database) {

    suspend fun ratingsOf(userId: Long, nowMillis: Long = System.currentTimeMillis()): List<RatingEntry> = dbQuery {
        val stored = readAll(userId, nowMillis)
        GameSpeed.entries.map { speed ->
            val (rating, games) = stored[speed] ?: (Glicko2Rating() to 0)
            RatingEntry(
                speed = speed,
                rating = rating.rating.roundToInt(),
                deviation = rating.deviation.roundToInt(),
                games = games,
                provisional = Glicko2.isProvisional(rating.deviation)
            )
        }
    }

    suspend fun applyResult(
        speed: GameSpeed,
        whiteUserId: Long,
        blackUserId: Long,
        whiteScore: Double,
        nowMillis: Long = System.currentTimeMillis()
    ): List<RatingChange> = dbQuery {
        if (whiteUserId == blackUserId) {
            return@dbQuery emptyList()
        }
        val white = read(whiteUserId, speed, nowMillis)
        val black = read(blackUserId, speed, nowMillis)
        val updatedWhite = Glicko2.update(white.rating, black.rating, whiteScore)
        val updatedBlack = Glicko2.update(black.rating, white.rating, 1.0 - whiteScore)
        write(whiteUserId, speed, updatedWhite, white.games + 1, nowMillis)
        write(blackUserId, speed, updatedBlack, black.games + 1, nowMillis)
        listOf(
            RatingChange(whiteUserId, speed, white.rating.rating.roundToInt(), updatedWhite.rating.roundToInt()),
            RatingChange(blackUserId, speed, black.rating.rating.roundToInt(), updatedBlack.rating.roundToInt())
        )
    }

    suspend fun leaderboard(speed: GameSpeed, limit: Int = 50): List<LeaderboardEntry> = dbQuery {
        (UserRatings innerJoin Users)
            .selectAll()
            .where { (UserRatings.speed eq speed.id) and (UserRatings.games greaterEq MIN_RANKED_GAMES) }
            .orderBy(UserRatings.rating, SortOrder.DESC)
            .limit(limit.coerceIn(1, 200))
            .map { row ->
                LeaderboardEntry(
                    userId = row[UserRatings.userId],
                    displayName = row[Users.displayName],
                    rating = row[UserRatings.rating].roundToInt(),
                    games = row[UserRatings.games],
                    provisional = Glicko2.isProvisional(row[UserRatings.deviation])
                )
            }
    }

    suspend fun ratingValue(userId: Long, speed: GameSpeed, nowMillis: Long = System.currentTimeMillis()): Int =
        dbQuery { read(userId, speed, nowMillis).rating.rating.roundToInt() }

    private data class StoredRating(val rating: Glicko2Rating, val games: Int)

    private fun read(userId: Long, speed: GameSpeed, nowMillis: Long): StoredRating {
        val row = UserRatings.selectAll()
            .where { (UserRatings.userId eq userId) and (UserRatings.speed eq speed.id) }
            .firstOrNull() ?: return StoredRating(Glicko2Rating(), 0)
        val stored = Glicko2Rating(
            rating = row[UserRatings.rating],
            deviation = row[UserRatings.deviation],
            volatility = row[UserRatings.volatility]
        )
        val idle = (nowMillis - row[UserRatings.lastPlayedMillis]).coerceAtLeast(0)
        return StoredRating(Glicko2.decayed(stored, idle), row[UserRatings.games])
    }

    private fun readAll(userId: Long, nowMillis: Long): Map<GameSpeed, Pair<Glicko2Rating, Int>> =
        UserRatings.selectAll()
            .where { UserRatings.userId eq userId }
            .mapNotNull { row ->
                val speed = GameSpeed.byId(row[UserRatings.speed]) ?: return@mapNotNull null
                val stored = Glicko2Rating(
                    rating = row[UserRatings.rating],
                    deviation = row[UserRatings.deviation],
                    volatility = row[UserRatings.volatility]
                )
                val idle = (nowMillis - row[UserRatings.lastPlayedMillis]).coerceAtLeast(0)
                speed to (Glicko2.decayed(stored, idle) to row[UserRatings.games])
            }
            .toMap()

    private fun write(userId: Long, speed: GameSpeed, rating: Glicko2Rating, games: Int, nowMillis: Long) {
        UserRatings.upsert(UserRatings.userId, UserRatings.speed) { row ->
            row[UserRatings.userId] = userId
            row[UserRatings.speed] = speed.id
            row[UserRatings.rating] = rating.rating
            row[UserRatings.deviation] = rating.deviation
            row[UserRatings.volatility] = rating.volatility
            row[UserRatings.games] = games
            row[lastPlayedMillis] = nowMillis
        }
    }

    private suspend fun <T> dbQuery(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction(database) { block() }
    }

    companion object {
        const val MIN_RANKED_GAMES = 5
    }
}
