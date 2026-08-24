package dev.hawk0f.checkmates.session

import dev.hawk0f.checkmates.net.ApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private class FakeCrashStorage(traces: List<String>) : CrashStorageLike {
    var cleared = false
    private val stored = traces.toMutableList()
    override fun pendingTraces(): List<String> = stored.toList()
    override fun clearPending() {
        cleared = true
        stored.clear()
    }

    override val platformName: String = "android"
    override val platformVersion: String = "16"
    override val appVersion: String = "1.0.0"
}

class CrashUploaderTest {

    private fun apiWith(engine: MockEngine): ApiClient = ApiClient(
        HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
    )

    @Test
    fun pendingTracesAreUploadedAndCleared() = runTest {
        val requests = mutableListOf<String>()
        val engine = MockEngine { request ->
            requests += request.url.encodedPath
            respond("", HttpStatusCode.NoContent)
        }
        val storage = FakeCrashStorage(listOf("boom", "kaboom"))

        assertEquals(2, CrashUploader.uploadPending(apiWith(engine), storage))
        assertEquals(listOf("/api/crash", "/api/crash"), requests)
        assertTrue(storage.cleared)
    }

    @Test
    fun nothingIsSentWhenThereAreNoCrashes() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("", HttpStatusCode.NoContent)
        }
        val storage = FakeCrashStorage(emptyList())

        assertEquals(0, CrashUploader.uploadPending(apiWith(engine), storage))
        assertEquals(0, calls)
        assertFalse(storage.cleared)
    }

    @Test
    fun tracesAreKeptWhenTheServerRejectsThem() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val storage = FakeCrashStorage(listOf("boom"))

        assertEquals(0, CrashUploader.uploadPending(apiWith(engine), storage))
        assertFalse(storage.cleared)
        assertEquals(listOf("boom"), storage.pendingTraces())
    }

    @Test
    fun theReportCarriesPlatformDetails() = runTest {
        var body: String? = null
        val engine = MockEngine { request ->
            body = (request.body as TextContent).text
            respond("", HttpStatusCode.NoContent)
        }
        CrashUploader.uploadPending(apiWith(engine), FakeCrashStorage(listOf("boom")))

        val payload = body.orEmpty()
        assertTrue(payload.contains("\"platform\":\"android\""), payload)
        assertTrue(payload.contains("\"appVersion\":\"1.0.0\""), payload)
        assertTrue(payload.contains("\"osVersion\":\"16\""), payload)
    }
}
