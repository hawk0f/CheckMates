package dev.hawk0f.chess.shared.protocol

import kotlin.random.Random

object ShortCode {

    const val LENGTH = 6
    const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"

    private val regex = Regex("[$ALPHABET]{$LENGTH}")

    fun generate(random: Random = Random.Default): String =
        buildString(LENGTH) {
            repeat(LENGTH) {
                append(ALPHABET[random.nextInt(ALPHABET.length)])
            }
        }

    fun isValid(code: String): Boolean = regex.matches(code)

    fun normalize(input: String): String = input.trim().uppercase()

    fun extractFromText(text: String): String? =
        Regex("/game/([$ALPHABET]{$LENGTH})").find(text)?.groupValues?.get(1)
            ?: normalize(text).takeIf { isValid(it) }
}
