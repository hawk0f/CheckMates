package dev.hawk0f.checkmates.shared.domain

import com.github.bhlangonijr.chesslib.move.MoveList

object PgnReader {

    fun uciMoves(pgn: String): List<String> {
        val body = strip(pgn)
        if (body.isBlank()) {
            return emptyList()
        }
        val list = MoveList()
        return runCatching {
            list.loadFromSan(body)
            list.map { it.toString() }
        }.getOrElse { emptyList() }
    }

    private fun strip(pgn: String): String = pgn
        .lines()
        .filterNot { it.trimStart().startsWith("[") }
        .joinToString(" ")
        .replace(Regex("\\{[^}]*\\}"), " ")
        .replace(Regex("\\d+\\.(\\.\\.)?"), " ")
        .replace(Regex("[?!]+"), " ")
        .replace(Regex("\\b(1-0|0-1|1/2-1/2|\\*)\\b"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
