package dev.hawk0f.checkmates.shared.domain

import com.github.bhlangonijr.chesslib.move.MoveList

object PgnReader {

    data class ImportedGame(
        val uciMoves: List<String>,
        val whiteName: String,
        val blackName: String,
        val winner: PieceColor?,
        val startFen: String?
    )

    fun read(pgn: String): ImportedGame? {
        val tags = pgn.lineSequence().mapNotNull(::parseTag).toMap()
        val startFen = tags["FEN"]?.takeIf { tags["SetUp"] == "1" }
        val moves = uciMoves(pgn, startFen)
        if (moves.isEmpty()) {
            return null
        }
        val result = tags["Result"] ?: resultTokens.firstOrNull { token ->
            Regex("(^|\\s)${Regex.escape(token)}(\\s|$)").containsMatchIn(pgn)
        }
        return ImportedGame(
            uciMoves = moves,
            whiteName = tags["White"].orEmpty().ifBlank { "White" },
            blackName = tags["Black"].orEmpty().ifBlank { "Black" },
            winner = when (result) {
                "1-0" -> PieceColor.WHITE
                "0-1" -> PieceColor.BLACK
                else -> null
            },
            startFen = startFen
        )
    }

    fun uciMoves(pgn: String): List<String> {
        val tags = pgn.lineSequence().mapNotNull(::parseTag).toMap()
        val startFen = tags["FEN"]?.takeIf { tags["SetUp"] == "1" }
        return uciMoves(pgn, startFen)
    }

    private fun uciMoves(pgn: String, startFen: String?): List<String> {
        val body = strip(pgn)
        if (body.isBlank()) {
            return emptyList()
        }
        val list = if (startFen == null) MoveList() else MoveList(startFen)
        return runCatching {
            list.loadFromSan(body)
            list.map { it.toString() }
        }.getOrElse { emptyList() }
    }

    private val resultTokens = setOf("1-0", "0-1", "1/2-1/2", "*")

    private fun parseTag(line: String): Pair<String, String>? {
        val match = Regex("^\\s*\\[([A-Za-z0-9_]+)\\s+\"(.*)\"\\]\\s*$").matchEntire(line) ?: return null
        val value = match.groupValues[2]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
        return match.groupValues[1] to value
    }

    private fun strip(pgn: String): String = pgn
        .lines()
        .filterNot { it.trimStart().startsWith("[") }
        .joinToString(" ")
        .replace(Regex("\\{[^}]*\\}"), " ")
        .let(::dropVariations)
        .replace(Regex("\\d+\\.(\\.\\.)?"), " ")
        .replace(Regex("[?!]+"), " ")
        .replace(Regex("\\$\\d+"), " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() && it !in resultTokens }
        .joinToString(" ")
        .trim()

    private fun dropVariations(text: String): String {
        val builder = StringBuilder()
        var depth = 0
        for (character in text) {
            when (character) {
                '(' -> depth++
                ')' -> if (depth > 0) depth-- else builder.append(' ')
                else -> if (depth == 0) builder.append(character)
            }
        }
        return builder.toString()
    }
}
