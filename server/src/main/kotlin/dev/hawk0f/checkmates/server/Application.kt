package dev.hawk0f.checkmates.server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import dev.hawk0f.checkmates.shared.protocol.ApiError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val AuthRateLimit = RateLimitName("auth")
val RoomRateLimit = RateLimitName("rooms")
val UploadRateLimit = RateLimitName("uploads")

private const val BACKUP_INTERVAL_HOURS = 12L
private const val MAX_BODY_BYTES = 256 * 1024L
private const val MAX_FRAME_BYTES = 64 * 1024L

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    val publicBaseUrl = System.getenv("PUBLIC_BASE_URL") ?: "http://localhost:8080"
    val dbPath = System.getenv("DB_PATH") ?: "data/chess.db"
    val database = Db.init(dbPath)
    val users = UserRepository(database)
    val roomStore = SqliteRoomStore(database)
    val ratings = RatingRepository(database)
    val crashes = CrashRepository(database)
    val adminToken = System.getenv("ADMIN_TOKEN")
    val backupDirectory = System.getenv("BACKUP_DIR") ?: "data/backups"
    val startedAtMillis = System.currentTimeMillis()
    val registry = RoomRegistry(
        publicBaseUrl = publicBaseUrl,
        recorder = GameRecorder(users::insertGame),
        store = roomStore,
        ratings = ratings::applyResult
    )

    install(XForwardedHeaders) {
        useLastProxy()
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }
    }
    install(StatusPages) {
        exception<BodyTooLargeException> { call, cause ->
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                ApiError("BODY_TOO_LARGE", "request body must not exceed ${cause.maxBytes} bytes")
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("BAD_REQUEST", "malformed request"))
        }
        exception<SerializationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("BAD_REQUEST", "malformed request body"))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("unhandled failure on ${call.request.path()}", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("INTERNAL", "internal error"))
        }
    }
    install(RateLimit) {
        register(AuthRateLimit) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { call -> call.clientKey() }
        }
        register(RoomRateLimit) {
            rateLimiter(limit = 30, refillPeriod = 1.minutes)
            requestKey { call -> call.clientKey() }
        }
        register(UploadRateLimit) {
            rateLimiter(limit = 60, refillPeriod = 1.minutes)
            requestKey { call -> call.clientKey() }
        }
    }
    install(BodySizeLimit) {
        maxBytes = MAX_BODY_BYTES
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 30.seconds
        maxFrameSize = MAX_FRAME_BYTES
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }

    launch {
        val restored = registry.restoreFromStore()
        log.info("restored $restored resumable rooms")
    }

    launch {
        while (true) {
            delay(10.minutes)
            registry.cleanup()
            users.purgeExpiredSessions()
        }
    }

    launch {
        val backup = SqliteBackup(dbPath, backupDirectory)
        while (true) {
            runCatching {
                backup.run()
                crashes.purgeOlderThan()
            }.onFailure { error -> log.error("backup failed", error) }
            delay(BACKUP_INTERVAL_HOURS.hours)
        }
    }

    configureRouting(
        registry = registry,
        users = users,
        ratings = ratings,
        seekPool = SeekPool(registry),
        crashes = crashes,
        adminToken = adminToken,
        startedAtMillis = startedAtMillis
    )
}
