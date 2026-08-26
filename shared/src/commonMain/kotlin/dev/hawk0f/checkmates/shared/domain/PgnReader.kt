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

    private val resultTokens = setOf("1-0", "0-1", "1/2-1/2", "*")

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
