package dev.hawk0f.checkmates.net

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

actual fun platformHttpClient(idleTimeoutMillis: Long): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            readTimeout(idleTimeoutMillis, TimeUnit.MILLISECONDS)
            pingInterval(20, TimeUnit.SECONDS)
        }
    }
}
