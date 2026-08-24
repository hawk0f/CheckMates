package dev.hawk0f.checkmates.net

import dev.hawk0f.checkmates.shared.protocol.SeekJson
import dev.hawk0f.checkmates.shared.protocol.SeekMessage
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.encodeURLParameter
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
            append(name.encodeURLParameter())
            if (authToken != null) {
                append("&token=")
                append(authToken.encodeURLParameter())
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
