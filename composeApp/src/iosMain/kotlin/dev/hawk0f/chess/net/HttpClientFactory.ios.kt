package dev.hawk0f.chess.net

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpClient(): HttpClient = HttpClient(Darwin)
