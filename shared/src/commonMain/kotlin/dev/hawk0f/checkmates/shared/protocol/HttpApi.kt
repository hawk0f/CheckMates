package dev.hawk0f.checkmates.shared.protocol

import kotlinx.serialization.Serializable
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor

@Serializable
data class TimeControl(val initialSeconds: Int, val incrementSeconds: Int) {
    val label: String
        get() = "${initialSeconds / 60}+$incrementSeconds"
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
data class ApiError(val code: String, val message: String)
