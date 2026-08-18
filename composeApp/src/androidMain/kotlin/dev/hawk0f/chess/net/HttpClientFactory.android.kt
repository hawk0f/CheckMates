package dev.hawk0f.chess.net

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun platformHttpClient(): HttpClient = HttpClient(OkHttp)
