package dev.hawk0f.checkmates.ui.openings

import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.domain.PgnReader
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.opening.OpeningLine

object OpeningLineImporter {

    fun import(name: String, pgn: String, trainedColor: PieceColor): OpeningLine? {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) {
            return null
        }
        val moves = PgnReader.uciMoves(pgn)
        if (moves.isEmpty()) {
            return null
        }
        val game = ChessGame()
        if (moves.any { game.applyUci(it) !is MoveOutcome.Applied }) {
            return null
        }
        val identity = "$normalizedName|${trainedColor.name}|${moves.joinToString(",")}".hashCode().toUInt().toString(16)
        return OpeningLine(
            id = "custom-$identity",
            name = normalizedName,
            trainedColor = trainedColor,
            moves = moves
        )
    }
}
