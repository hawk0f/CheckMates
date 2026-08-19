package dev.hawk0f.checkmates.shared.domain

data class GameState(
    val fen: String,
    val pieces: Map<Square, Piece>,
    val sideToMove: PieceColor,
    val lastMove: Pair<Square, Square>?,
    val inCheck: Boolean,
    val result: GameResult?,
    val uciHistory: List<String>
)
