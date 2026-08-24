package dev.hawk0f.checkmates.server

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeStringUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BodySizeLimitTest {

    private val limit = 1024L

    private fun echoApp(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            install(BodySizeLimit) {
                maxBytes = limit
            }
            install(io.ktor.server.plugins.statuspages.StatusPages) {
                exception<BodyTooLargeException> { call, cause ->
                    call.respondText(
                        text = "too large: ${cause.maxBytes}",
                        status = HttpStatusCode.PayloadTooLarge
                    )
                }
            }
            routing {
                post("/echo") {
                    call.respondText("received ${call.receiveText().length}")
                }
            }
        }
        block()
    }

    @Test
    fun bodiesUnderTheLimitPassThrough() = echoApp {
        val response = client.post("/echo") {
            contentType(ContentType.Text.Plain)
            setBody("x".repeat(100))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("received 100", response.bodyAsText())
    }

    @Test
    fun aDeclaredOversizedBodyIsRejectedOnce() = echoApp {
        val response = client.post("/echo") {
            contentType(ContentType.Text.Plain)
            setBody("x".repeat((limit + 1).toInt()))
        }
        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertEquals("too large: $limit", response.bodyAsText())
    }

    @Test
    fun anOversizedChunkedBodyIsRejected() = echoApp {
        val channel = ByteChannel(autoFlush = true)
        val response = client.post("/echo") {
            contentType(ContentType.Text.Plain)
            setBody(channel)
            channel.writeStringUtf8("x".repeat((limit + 512).toInt()))
            channel.close()
        }
        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertTrue(response.bodyAsText().startsWith("too large"))
    }
}
