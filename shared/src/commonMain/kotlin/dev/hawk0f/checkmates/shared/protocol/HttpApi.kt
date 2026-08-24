package dev.hawk0f.checkmates.shared.protocol

import kotlinx.serialization.Serializable
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor

@Serializable
enum class ClockMode(val id: String) {
    FISCHER("fischer"),
    BRONSTEIN("bronstein"),
    DELAY("delay");

    companion object {
        fun byId(id: String?): ClockMode = entries.find { it.id == id } ?: FISCHER
    }
}

@Serializable
data class TimeControl(
    val initialSeconds: Int,
    val incrementSeconds: Int,
    val mode: ClockMode = ClockMode.FISCHER,
    val blackInitialSeconds: Int? = null,
    val blackIncrementSeconds: Int? = null
) {
    val label: String
        get() {
            val base = "${initialSeconds / 60}+$incrementSeconds"
            val suffix = when (mode) {
                ClockMode.FISCHER -> ""
                ClockMode.BRONSTEIN -> " B"
                ClockMode.DELAY -> " D"
            }
            val odds = if (hasOdds) " ⚖" else ""
            return base + suffix + odds
        }

    val hasOdds: Boolean
        get() = blackInitialSeconds != null || blackIncrementSeconds != null

    fun initialSecondsFor(color: PieceColor): Int =
        if (color == PieceColor.BLACK) blackInitialSeconds ?: initialSeconds else initialSeconds

    fun incrementSecondsFor(color: PieceColor): Int =
        if (color == PieceColor.BLACK) blackIncrementSeconds ?: incrementSeconds else incrementSeconds
}

@Serializable
data class CreateGameRequest(val hostName: String, val timeControl: TimeControl? = null)

@Serializable
data class CreateGameResponse(
    val gameId: String,
    val shortCode: String,
    val joinUrl: String,
    val playerToken: String
)

@Serializable
data class GameInfoResponse(
    val exists: Boolean,
    val joinable: Boolean,
    val gameId: String?,
    val hostName: String?,
    val timeControl: TimeControl? = null
)

@Serializable
data class RegisterRequest(
    val login: String,
    val password: String,
    val displayName: String
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class ProfileResponse(
    val id: Long,
    val login: String,
    val displayName: String,
    val avatarKind: String,
    val avatarValue: String,
    val createdAtMillis: Long
)

@Serializable
data class AuthResponse(
    val token: String,
    val profile: ProfileResponse
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val avatarKind: String? = null,
    val avatarValue: String? = null
)

@Serializable
data class GameRecordRequest(
    val mode: String,
    val myColor: PieceColor?,
    val whiteName: String,
    val blackName: String,
    val winner: PieceColor?,
    val reason: GameOverReason,
    val uciHistory: List<String>,
    val finishedAtMillis: Long
)

@Serializable
data class GameRecordResponse(val id: Long)

@Serializable
data class GameHistoryItem(
    val id: Long,
    val mode: String,
    val myColor: PieceColor?,
    val whiteName: String,
    val blackName: String,
    val winner: PieceColor?,
    val reason: GameOverReason,
    val uciHistory: List<String>,
    val finishedAtMillis: Long
)

@Serializable
data class GameHistoryResponse(val games: List<GameHistoryItem>)

@Serializable
data class FriendSummary(
    val userId: Long,
    val displayName: String,
    val login: String = "",
    val online: Boolean = false,
    val lastPlayedMillis: Long? = null
)

@Serializable
data class FriendsResponse(
    val friends: List<FriendSummary>,
    val recentOpponents: List<FriendSummary>
)

@Serializable
data class AddFriendRequest(val query: String)

@Serializable
data class PushTokenRequest(val token: String)

@Serializable
data class ChallengeRequest(
    val friendUserId: Long,
    val timeControl: TimeControl? = null
)

@Serializable
data class ChallengeResponse(
    val gameId: String,
    val shortCode: String,
    val joinUrl: String,
    val playerToken: String,
    val pushed: Boolean
)

@Serializable
data class CrashReportRequest(
    val platform: String,
    val appVersion: String,
    val osVersion: String,
    val stackTrace: String,
    val occurredAtMillis: Long
)

@Serializable
data class CrashReportItem(
    val id: Long,
    val platform: String,
    val appVersion: String,
    val osVersion: String,
    val stackTrace: String,
    val occurredAtMillis: Long,
    val receivedAtMillis: Long
)

@Serializable
data class CrashReportsResponse(val reports: List<CrashReportItem>)

@Serializable
data class ApiError(val code: String, val message: String)
