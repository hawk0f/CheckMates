package dev.hawk0f.checkmates.net

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val REST_TIMEOUT_MILLIS = 20_000L
const val NO_IDLE_TIMEOUT_MILLIS = 0L

expect fun platformHttpClient(idleTimeoutMillis: Long = REST_TIMEOUT_MILLIS): HttpClient

fun configuredHttpClient(): HttpClient = platformHttpClient(REST_TIMEOUT_MILLIS).config {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = REST_TIMEOUT_MILLIS
        connectTimeoutMillis = REST_TIMEOUT_MILLIS
        socketTimeoutMillis = REST_TIMEOUT_MILLIS
    }
}

fun configuredWebSocketClient(): HttpClient = platformHttpClient(NO_IDLE_TIMEOUT_MILLIS).config {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(WebSockets)
}
