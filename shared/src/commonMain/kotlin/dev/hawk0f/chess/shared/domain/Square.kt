package dev.hawk0f.chess.shared.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Square(val index: Int) {

    val file: Int get() = index % 8
    val rank: Int get() = index / 8

    fun toUci(): String = "${'a' + file}${rank + 1}"

    companion object {
        fun of(file: Int, rank: Int): Square {
            require(file in 0..7 && rank in 0..7) { "Square out of board: file=$file rank=$rank" }
            return Square(rank * 8 + file)
        }

        fun fromUci(uci: String): Square {
            require(uci.length == 2) { "Bad square: $uci" }
            val file = uci[0] - 'a'
            val rank = uci[1] - '1'
            return of(file, rank)
        }
    }
}
