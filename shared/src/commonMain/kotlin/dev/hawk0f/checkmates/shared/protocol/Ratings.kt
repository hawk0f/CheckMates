package dev.hawk0f.checkmates.shared.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class GameSpeed(val id: String) {
    BULLET("bullet"),
    BLITZ("blitz"),
    RAPID("rapid"),
    CLASSICAL("classical");

    companion object {
        fun byId(id: String?): GameSpeed? = entries.find { it.id == id }

        fun of(timeControl: TimeControl?): GameSpeed {
            if (timeControl == null) {
                return CLASSICAL
            }
            val estimate = timeControl.initialSeconds + 40 * timeControl.incrementSeconds
            return when {
                estimate < 180 -> BULLET
                estimate < 480 -> BLITZ
                estimate < 1500 -> RAPID
                else -> CLASSICAL
            }
        }
    }
}

@Serializable
data class RatingEntry(
    val speed: GameSpeed,
    val rating: Int,
    val deviation: Int,
    val games: Int,
    val provisional: Boolean
)

@Serializable
data class RatingsResponse(val ratings: List<RatingEntry>)

@Serializable
data class LeaderboardEntry(
    val userId: Long,
    val displayName: String,
    val rating: Int,
    val games: Int,
    val provisional: Boolean
)

@Serializable
data class LeaderboardResponse(
    val speed: GameSpeed,
    val entries: List<LeaderboardEntry>
)
