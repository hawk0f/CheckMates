package dev.hawk0f.checkmates.shared.opening

import dev.hawk0f.checkmates.shared.domain.PieceColor
import kotlinx.serialization.Serializable

@Serializable
data class OpeningLine(
    val id: String,
    val name: String,
    val trainedColor: PieceColor,
    val moves: List<String>
) {
    val plies: Int get() = moves.size
}

object OpeningBook {

    val lines: List<OpeningLine> = listOf(
        OpeningLine(
            id = "italian",
            name = "Italian Game",
            trainedColor = PieceColor.WHITE,
            moves = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "c2c3", "g8f6")
        ),
        OpeningLine(
            id = "ruy-lopez",
            name = "Ruy Lopez",
            trainedColor = PieceColor.WHITE,
            moves = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5a4", "g8f6", "e1g1")
        ),
        OpeningLine(
            id = "scotch",
            name = "Scotch Game",
            trainedColor = PieceColor.WHITE,
            moves = listOf("e2e4", "e7e5", "g1f3", "b8c6", "d2d4", "e5d4", "f3d4", "f8c5")
        ),
        OpeningLine(
            id = "queens-gambit-declined",
            name = "Queen's Gambit Declined",
            trainedColor = PieceColor.WHITE,
            moves = listOf("d2d4", "d7d5", "c2c4", "e7e6", "b1c3", "g8f6", "c1g5", "f8e7")
        ),
        OpeningLine(
            id = "london",
            name = "London System",
            trainedColor = PieceColor.WHITE,
            moves = listOf("d2d4", "d7d5", "c1f4", "g8f6", "e2e3", "e7e6", "g1f3", "f8d6")
        ),
        OpeningLine(
            id = "sicilian-open",
            name = "Open Sicilian",
            trainedColor = PieceColor.BLACK,
            moves = listOf("e2e4", "c7c5", "g1f3", "d7d6", "d2d4", "c5d4", "f3d4", "g8f6", "b1c3", "a7a6")
        ),
        OpeningLine(
            id = "french",
            name = "French Defence",
            trainedColor = PieceColor.BLACK,
            moves = listOf("e2e4", "e7e6", "d2d4", "d7d5", "b1c3", "g8f6", "c1g5", "f8e7")
        ),
        OpeningLine(
            id = "caro-kann",
            name = "Caro-Kann Defence",
            trainedColor = PieceColor.BLACK,
            moves = listOf("e2e4", "c7c6", "d2d4", "d7d5", "b1c3", "d5e4", "c3e4", "c8f5")
        ),
        OpeningLine(
            id = "kings-indian",
            name = "King's Indian Defence",
            trainedColor = PieceColor.BLACK,
            moves = listOf("d2d4", "g8f6", "c2c4", "g7g6", "b1c3", "f8g7", "e2e4", "d7d6", "g1f3", "e8g8")
        ),
        OpeningLine(
            id = "scandinavian",
            name = "Scandinavian Defence",
            trainedColor = PieceColor.BLACK,
            moves = listOf("e2e4", "d7d5", "e4d5", "d8d5", "b1c3", "d5a5", "d2d4", "g8f6")
        )
    )

    fun byId(id: String): OpeningLine? = lines.find { it.id == id }

    fun forColor(color: PieceColor): List<OpeningLine> = lines.filter { it.trainedColor == color }
}
