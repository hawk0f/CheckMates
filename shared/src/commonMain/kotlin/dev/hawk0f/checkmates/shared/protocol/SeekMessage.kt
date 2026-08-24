package dev.hawk0f.checkmates.shared.protocol

import dev.hawk0f.checkmates.shared.domain.PieceColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface SeekMessage {

    @Serializable
    @SerialName("waiting")
    data class Waiting(val queued: Int, val rating: Int, val speed: GameSpeed) : SeekMessage

    @Serializable
    @SerialName("matched")
    data class Matched(
        val gameId: String,
        val shortCode: String,
        val playerToken: String,
        val color: PieceColor,
        val opponentName: String,
        val opponentRating: Int
    ) : SeekMessage

    @Serializable
    @SerialName("seekError")
    data class Error(val code: String, val message: String) : SeekMessage
}

object SeekJson {
    val json: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(message: SeekMessage): String = json.encodeToString(SeekMessage.serializer(), message)

    fun decode(text: String): SeekMessage = json.decodeFromString(SeekMessage.serializer(), text)
}
