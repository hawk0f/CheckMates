package dev.hawk0f.checkmates.server

import com.google.auth.oauth2.GoogleCredentials
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory

object FcmSender {

    private val logger = LoggerFactory.getLogger(FcmSender::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = HttpClient.newHttpClient()

    private val keyFile = File(System.getenv("FCM_CREDENTIALS") ?: "/app/fcm-key.json")

    private val projectId: String? = runCatching {
        Json.parseToJsonElement(keyFile.readText()).jsonObject["project_id"]?.jsonPrimitive?.content
    }.getOrNull()

    private val credentials: GoogleCredentials? = runCatching {
        GoogleCredentials.fromStream(keyFile.inputStream())
            .createScoped("https://www.googleapis.com/auth/firebase.messaging")
    }.getOrNull()

    val enabled: Boolean = projectId != null && credentials != null

    init {
        if (enabled) {
            logger.info("FCM enabled for project {}", projectId)
        } else {
            logger.info("FCM disabled: no credentials at {}", keyFile.path)
        }
    }

    fun send(token: String, title: String, body: String, data: Map<String, String> = emptyMap()) {
        if (!enabled) {
            return
        }
        scope.launch {
            runCatching {
                val activeCredentials = credentials ?: return@launch
                activeCredentials.refreshIfExpired()
                val accessToken = activeCredentials.accessToken?.tokenValue ?: return@launch
                val payload = buildJsonObject {
                    putJsonObject("message") {
                        put("token", token)
                        putJsonObject("notification") {
                            put("title", title)
                            put("body", body)
                        }
                        putJsonObject("android") {
                            put("priority", "HIGH")
                        }
                        if (data.isNotEmpty()) {
                            putJsonObject("data") {
                                for ((key, value) in data) {
                                    put(key, value)
                                }
                            }
                        }
                    }
                }
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("https://fcm.googleapis.com/v1/projects/$projectId/messages:send"))
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build()
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() !in 200..299) {
                    logger.warn("FCM send failed {}: {}", response.statusCode(), response.body().take(300))
                }
            }.onFailure { error ->
                logger.warn("FCM send error: {}", error.message)
            }
        }
    }
}
