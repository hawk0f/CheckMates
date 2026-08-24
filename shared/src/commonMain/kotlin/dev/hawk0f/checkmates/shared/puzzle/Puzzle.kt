package dev.hawk0f.checkmates.shared.puzzle

import kotlinx.serialization.Serializable

@Serializable
enum class PuzzleTheme(val id: String) {
    MATE_IN_ONE("mateIn1"),
    MATE_IN_TWO("mateIn2"),
    FORK("fork"),
    PIN("pin"),
    SKEWER("skewer"),
    BACK_RANK("backRank"),
    PROMOTION("promotion"),
    HANGING_PIECE("hangingPiece")
}

@Serializable
data class Puzzle(
    val id: String,
    val fen: String,
    val solution: List<String>,
    val rating: Int,
    val theme: PuzzleTheme
)
