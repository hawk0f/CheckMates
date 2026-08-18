package dev.hawk0f.chess.server

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    val publicBaseUrl = System.getenv("PUBLIC_BASE_URL") ?: "http://localhost:8080"
    val registry = RoomRegistry(publicBaseUrl)

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 30.seconds
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }

    launch {
        while (true) {
            delay(10.minutes)
            registry.cleanup()
        }
    }

    configureRouting(registry)
}
