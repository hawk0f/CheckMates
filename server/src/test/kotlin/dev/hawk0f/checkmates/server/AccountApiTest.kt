package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.AuthResponse
import dev.hawk0f.checkmates.shared.protocol.GameHistoryResponse
import dev.hawk0f.checkmates.shared.protocol.GameRecordRequest
import dev.hawk0f.checkmates.shared.protocol.LoginRequest
import dev.hawk0f.checkmates.shared.protocol.ProfileResponse
import dev.hawk0f.checkmates.shared.protocol.RegisterRequest
import dev.hawk0f.checkmates.shared.protocol.UpdateProfileRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class AccountApiTest {

    private fun withAccountApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        val dbPath = Files.createTempDirectory("chess-test").resolve("test.db").toString()
        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            routing {
                accountRoutes(UserRepository(Db.init(dbPath)))
            }
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        block(client)
    }

    @Test
    fun registerLoginProfileAndHistory() = withAccountApp { client ->
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("hawk0f", "secret123", "Hawk"))
        }
        assertEquals(HttpStatusCode.OK, registerResponse.status)
        val registered = registerResponse.body<AuthResponse>()
        assertEquals("hawk0f", registered.profile.login)
        assertEquals("Hawk", registered.profile.displayName)

        val duplicate = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("hawk0f", "secret123", "Other"))
        }
        assertEquals(HttpStatusCode.BadRequest, duplicate.status)

        val badLogin = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("hawk0f", "wrongpass"))
        }
        assertEquals(HttpStatusCode.BadRequest, badLogin.status)

        val login = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("hawk0f", "secret123"))
        }.body<AuthResponse>()

        val me = client.get("/api/me") {
            bearerAuth(login.token)
        }.body<ProfileResponse>()
        assertEquals("hawk0f", me.login)

        val updated = client.patch("/api/me") {
            bearerAuth(login.token)
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(displayName = "Hawk the Great", avatarKind = "emoji", avatarValue = "🦅"))
        }.body<ProfileResponse>()
        assertEquals("Hawk the Great", updated.displayName)
        assertEquals("emoji", updated.avatarKind)

        val record = client.post("/api/me/games") {
            bearerAuth(login.token)
            contentType(ContentType.Application.Json)
            setBody(
                GameRecordRequest(
                    mode = "online",
                    myColor = PieceColor.WHITE,
                    whiteName = "Hawk",
                    blackName = "Rival",
                    winner = PieceColor.WHITE,
                    reason = GameOverReason.CHECKMATE,
                    uciHistory = listOf("e2e4", "e7e5", "d1h5", "b8c6", "f1c4", "g8f6", "h5f7"),
                    finishedAtMillis = 1000L
                )
            )
        }
        assertEquals(HttpStatusCode.OK, record.status)

        val history = client.get("/api/me/games") {
            bearerAuth(login.token)
        }.body<GameHistoryResponse>()
        assertEquals(1, history.games.size)
        assertEquals(7, history.games.first().uciHistory.size)
        assertEquals(PieceColor.WHITE, history.games.first().winner)

        val unauthorized = client.get("/api/me/games")
        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)

        assertTrue(client.get("/api/me") { bearerAuth("deadbeef") }.status == HttpStatusCode.Unauthorized)
    }
}
