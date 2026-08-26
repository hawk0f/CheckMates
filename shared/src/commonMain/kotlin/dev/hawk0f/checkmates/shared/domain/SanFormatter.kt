package dev.hawk0f.checkmates.shared.domain

import com.github.bhlangonijr.chesslib.move.MoveList

object SanFormatter {

    fun sanMoves(uciHistory: List<String>, startFen: String? = null): List<String> {
        if (uciHistory.isEmpty()) {
            return emptyList()
        }
        val moveList = if (startFen == null) MoveList() else MoveList(startFen)
        return runCatching {
            moveList.loadFromText(uciHistory.joinToString(" "))
            moveList.toSanWithMoveNumbers()
                .split(Regex("\\s+"))
                .filter { token -> token.isNotBlank() && !token.endsWith(".") }
        }.getOrElse { uciHistory }
    }
}
