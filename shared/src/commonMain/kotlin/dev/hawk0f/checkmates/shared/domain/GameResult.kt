package dev.hawk0f.checkmates.shared.domain

import kotlinx.serialization.Serializable

@Serializable
enum class GameOverReason {
    CHECKMATE,
    STALEMATE,
    DRAW_AGREED,
    RESIGNATION,
    INSUFFICIENT_MATERIAL,
    REPETITION,
    FIFTY_MOVE,
    TIMEOUT,
    DISCONNECTION
}

@Serializable
data class GameResult(val reason: GameOverReason, val winner: PieceColor?)
