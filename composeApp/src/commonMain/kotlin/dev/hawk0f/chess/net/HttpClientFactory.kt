package dev.hawk0f.chess.net

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun platformHttpClient(): HttpClient

fun configuredHttpClient(): HttpClient = platformHttpClient().config {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(WebSockets)
}
