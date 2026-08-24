package dev.hawk0f.checkmates.net

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

private const val NO_IDLE_TIMEOUT_SECONDS = 7.0 * 24 * 60 * 60

actual fun platformHttpClient(idleTimeoutMillis: Long): HttpClient = HttpClient(Darwin) {
    engine {
        configureSession {
            val seconds = if (idleTimeoutMillis <= 0) NO_IDLE_TIMEOUT_SECONDS else idleTimeoutMillis / 1000.0
            setTimeoutIntervalForRequest(seconds)
            setTimeoutIntervalForResource(seconds)
        }
    }
}
