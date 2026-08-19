package dev.hawk0f.checkmates.shared.domain

import com.github.bhlangonijr.chesslib.move.MoveList

object PgnBuilder {

    fun build(
        whiteName: String,
        blackName: String,
        winner: PieceColor?,
        reason: GameOverReason?,
        uciHistory: List<String>,
        dateMillis: Long? = null,
        event: String = "Casual game",
        site: String = "chess.hawk0f.icu"
    ): String {
        val resultTag = when {
            reason == null -> "*"
            winner == PieceColor.WHITE -> "1-0"
            winner == PieceColor.BLACK -> "0-1"
            else -> "1/2-1/2"
        }
        val movesText = if (uciHistory.isEmpty()) {
            ""
        } else {
            val moveList = MoveList()
            moveList.loadFromText(uciHistory.joinToString(" "))
            moveList.toSanWithMoveNumbers().trim()
        }
        val termination = when (reason) {
            GameOverReason.RESIGNATION -> "Normal"
            GameOverReason.TIMEOUT -> "Time forfeit"
            GameOverReason.DISCONNECTION -> "Abandoned"
            null -> "Unterminated"
            else -> "Normal"
        }
        return buildString {
            appendLine("[Event \"${escape(event)}\"]")
            appendLine("[Site \"${escape(site)}\"]")
            appendLine("[Date \"${dateMillis?.let(::pgnDate) ?: "????.??.??"}\"]")
            appendLine("[Round \"-\"]")
            appendLine("[White \"${escape(whiteName)}\"]")
            appendLine("[Black \"${escape(blackName)}\"]")
            appendLine("[Result \"$resultTag\"]")
            appendLine("[Termination \"$termination\"]")
            appendLine()
            if (movesText.isEmpty()) {
                appendLine(resultTag)
            } else {
                appendLine("$movesText $resultTag")
            }
        }
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun pgnDate(millis: Long): String {
        val days = millis.floorDiv(86_400_000L)
        val shifted = days + 719_468
        val era = shifted.floorDiv(146_097)
        val dayOfEra = shifted - era * 146_097
        val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
        val year = yearOfEra + era * 400
        val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val monthIndex = (5 * dayOfYear + 2) / 153
        val day = dayOfYear - (153 * monthIndex + 2) / 5 + 1
        val month = if (monthIndex < 10) monthIndex + 3 else monthIndex - 9
        val calendarYear = if (month <= 2) year + 1 else year
        val monthText = month.toString().padStart(2, '0')
        val dayText = day.toString().padStart(2, '0')
        return "$calendarYear.$monthText.$dayText"
    }
}
