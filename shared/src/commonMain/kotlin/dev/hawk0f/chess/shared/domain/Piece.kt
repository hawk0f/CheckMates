package dev.hawk0f.chess.shared.domain

import kotlinx.serialization.Serializable

@Serializable
enum class PieceColor {
    WHITE,
    BLACK;

    val opposite: PieceColor get() = if (this == WHITE) BLACK else WHITE
}

@Serializable
enum class PieceKind {
    PAWN,
    KNIGHT,
    BISHOP,
    ROOK,
    QUEEN,
    KING
}

@Serializable
data class Piece(val color: PieceColor, val kind: PieceKind)
