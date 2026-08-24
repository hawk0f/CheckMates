package dev.hawk0f.checkmates.server

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

class BodySizeLimitConfig {
    var maxBytes: Long = 256 * 1024
}

class BodyTooLargeException(val maxBytes: Long) : Exception("request body must not exceed $maxBytes bytes")

val BodySizeLimit = createApplicationPlugin("BodySizeLimit", ::BodySizeLimitConfig) {
    val maxBytes = pluginConfig.maxBytes
    onCallReceive { call ->
        val declared = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull()
        if (declared != null && declared > maxBytes) {
            throw BodyTooLargeException(maxBytes)
        }
        if (call.request.header(HttpHeaders.Upgrade) != null) {
            return@onCallReceive
        }
        transformBody { body: ByteReadChannel ->
            val bytes = body.readRemaining(maxBytes + 1).readByteArray()
            if (bytes.size > maxBytes) {
                throw BodyTooLargeException(maxBytes)
            }
            ByteReadChannel(bytes)
        }
    }
}

fun ApplicationCall.clientKey(): String = request.origin.remoteAddress
