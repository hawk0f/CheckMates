package dev.hawk0f.checkmates.net

import dev.hawk0f.checkmates.shared.protocol.SeekJson
import dev.hawk0f.checkmates.shared.protocol.SeekMessage
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SeekClient(private val client: HttpClient) {

    fun seek(name: String, timeControl: TimeControl, authToken: String?): Flow<SeekMessage> = callbackFlow {
        val url = buildString {
            append(ServerConfig.wsBaseUrl)
            append("/ws/seek?initial=")
            append(timeControl.initialSeconds)
            append("&increment=")
            append(timeControl.incrementSeconds)
            append("&name=")
            append(name.encodeQueryValue())
            if (authToken != null) {
                append("&token=")
                append(authToken.encodeQueryValue())
            }
        }
        client.webSocket(url) {
            for (frame in incoming) {
                if (frame !is Frame.Text) {
                    continue
                }
                val message = runCatching { SeekJson.decode(frame.readText()) }.getOrNull() ?: continue
                trySend(message)
                if (message is SeekMessage.Matched || message is SeekMessage.Error) {
                    break
                }
            }
        }
        close()
        awaitClose { }
    }
}

private fun String.encodeQueryValue(): String = buildString {
    for (byte in this@encodeQueryValue.encodeToByteArray()) {
        val value = byte.toInt() and 0xFF
        val char = value.toChar()
        if (char.isLetterOrDigit() || char in "-_.~") {
            append(char)
        } else {
            append('%')
            append(value.toString(16).uppercase().padStart(2, '0'))
        }
    }
}
