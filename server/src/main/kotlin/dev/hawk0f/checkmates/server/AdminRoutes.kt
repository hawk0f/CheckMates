package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.ApiError
import dev.hawk0f.checkmates.shared.protocol.CrashReportRequest
import dev.hawk0f.checkmates.shared.protocol.CrashReportsResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.security.MessageDigest
import kotlinx.serialization.Serializable

@Serializable
data class ServerMetrics(
    val uptimeSeconds: Long,
    val activeRooms: Int,
    val roomsInProgress: Int,
    val users: Long,
    val recordedGames: Long,
    val crashReports: Long
)

fun Route.adminRoutes(
    registry: RoomRegistry,
    users: UserRepository,
    crashes: CrashRepository?,
    startedAtMillis: Long,
    adminToken: String?
) {
    if (crashes != null) {
        rateLimit(UploadRateLimit) {
            post("/api/crash") {
                val request = call.receive<CrashReportRequest>()
                if (request.stackTrace.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("EMPTY_TRACE", "stack trace is empty"))
                    return@post
                }
                crashes.record(request)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        rateLimit(AuthRateLimit) {
            get("/api/admin/crashes") {
                if (!call.isAdmin(adminToken)) {
                    return@get
                }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                call.respond(CrashReportsResponse(crashes.recent(limit)))
            }
        }
    }

    rateLimit(AuthRateLimit) {
        get("/api/admin/metrics") {
            if (!call.isAdmin(adminToken)) {
                return@get
            }
            call.respond(
                ServerMetrics(
                    uptimeSeconds = (System.currentTimeMillis() - startedAtMillis) / 1000,
                    activeRooms = registry.size,
                    roomsInProgress = registry.countInProgress(),
                    users = users.countUsers(),
                    recordedGames = users.countGames(),
                    crashReports = crashes?.count() ?: 0
                )
            )
        }
    }
}

private fun constantTimeEquals(provided: String?, expected: String): Boolean {
    val providedBytes = (provided ?: "").toByteArray()
    val expectedBytes = expected.toByteArray()
    return MessageDigest.isEqual(providedBytes, expectedBytes)
}

private suspend fun ApplicationCall.isAdmin(adminToken: String?): Boolean {
    if (adminToken.isNullOrBlank()) {
        respond(HttpStatusCode.NotFound, ApiError("DISABLED", "admin endpoints are disabled"))
        return false
    }
    if (!constantTimeEquals(bearerToken(), adminToken)) {
        respond(HttpStatusCode.Unauthorized, ApiError("BAD_TOKEN", "admin token required"))
        return false
    }
    return true
}
