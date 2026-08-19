package dev.hawk0f.checkmates.shared.protocol

import kotlinx.serialization.json.Json

object ProtocolJson {

    val json: Json = Json {
        classDiscriminator = "t"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(message: GameMessage): String = json.encodeToString(GameMessage.serializer(), message)

    fun decode(text: String): GameMessage = json.decodeFromString(GameMessage.serializer(), text)
}
