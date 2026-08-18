package dev.hawk0f.chess.shared.protocol

import kotlinx.serialization.Serializable

@Serializable
data class CreateGameRequest(val hostName: String)

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
    val hostName: String?
)
