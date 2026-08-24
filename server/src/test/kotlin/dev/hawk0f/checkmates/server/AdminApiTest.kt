package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.CrashReportRequest
import dev.hawk0f.checkmates.shared.protocol.CrashReportsResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.websocket.WebSockets
import kotlin.time.Duration.Companion.minutes
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminApiTest {

    private fun ApplicationTestBuilder.client() = createClient {
        install(ContentNegotiation) { json() }
    }

    private fun newDatabase(): String {
        val file = File.createTempFile("admin-${UUID.randomUUID()}", ".db")
        file.delete()
        file.deleteOnExit()
        return file.absolutePath
    }

    private fun testModule(adminToken: String?): Application.() -> Unit = {
        val database = Db.init(newDatabase())
        val users = UserRepository(database)
        val crashes = CrashRepository(database)
        val registry = RoomRegistry("http://localhost")
        install(ServerContentNegotiation) {
            json()
        }
        install(RateLimit) {
            for (name in listOf(AuthRateLimit, RoomRateLimit, UploadRateLimit)) {
                register(name) {
                    rateLimiter(limit = 100, refillPeriod = 1.minutes)
                }
            }
        }
        install(WebSockets)
        configureRouting(
            registry = registry,
            users = users,
            crashes = crashes,
            adminToken = adminToken,
            startedAtMillis = System.currentTimeMillis() - 5_000
        )
    }

    private val report = CrashReportRequest(
        platform = "android",
        appVersion = "1.2.3",
        osVersion = "16",
        stackTrace = "java.lang.IllegalStateException: boom",
        occurredAtMillis = 1_700_000_000_000
    )

    @Test
    fun crashReportsArePostedAndListedForAdmins() = testApplication {
        application(testModule("secret"))
        val client = client()

        val posted = client.post("/api/crash") {
            contentType(ContentType.Application.Json)
            setBody(report)
        }
        assertEquals(HttpStatusCode.NoContent, posted.status)

        val listed = client.get("/api/admin/crashes") { bearerAuth("secret") }
        assertEquals(HttpStatusCode.OK, listed.status)
        val body = listed.body<CrashReportsResponse>()
        assertEquals(1, body.reports.size)
        assertEquals("java.lang.IllegalStateException: boom", body.reports.first().stackTrace)
    }

    @Test
    fun anEmptyStackTraceIsRejected() = testApplication {
        application(testModule("secret"))
        val response = client().post("/api/crash") {
            contentType(ContentType.Application.Json)
            setBody(report.copy(stackTrace = "   "))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun adminEndpointsNeedTheAdminToken() = testApplication {
        application(testModule("secret"))
        assertEquals(HttpStatusCode.Unauthorized, client().get("/api/admin/metrics").status)
        assertEquals(
            HttpStatusCode.Unauthorized,
            client().get("/api/admin/metrics") { bearerAuth("wrong") }.status
        )
    }

    @Test
    fun adminEndpointsAreHiddenWhenNoTokenIsConfigured() = testApplication {
        application(testModule(null))
        assertEquals(HttpStatusCode.NotFound, client().get("/api/admin/metrics").status)
    }

    @Test
    fun metricsReportUptimeAndCounters() = testApplication {
        application(testModule("secret"))
        val client = client()
        client.post("/api/crash") {
            contentType(ContentType.Application.Json)
            setBody(report)
        }

        val metrics = client.get("/api/admin/metrics") { bearerAuth("secret") }.body<ServerMetrics>()
        assertTrue(metrics.uptimeSeconds >= 5, "uptime was ${metrics.uptimeSeconds}")
        assertEquals(0, metrics.activeRooms)
        assertEquals(0, metrics.roomsInProgress)
        assertEquals(0L, metrics.users)
        assertEquals(1L, metrics.crashReports)
    }
}
