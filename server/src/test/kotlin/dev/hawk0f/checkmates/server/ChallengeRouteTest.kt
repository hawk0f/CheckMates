package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.ChallengeRequest
import dev.hawk0f.checkmates.shared.protocol.ChallengeResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
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
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.WebSockets
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class ChallengeRouteTest {

    private class Backend {
        val database = run {
            val file = File.createTempFile("challenge-${UUID.randomUUID()}", ".db")
            file.delete()
            file.deleteOnExit()
            Db.init(file.absolutePath)
        }
        val users = UserRepository(database)
        val friends = FriendRepository(database)
        val registry = RoomRegistry("http://localhost")
    }

    private fun ApplicationTestBuilder.client() = createClient {
        install(ContentNegotiation) { json() }
    }

    private fun testModule(backend: Backend): Application.() -> Unit = {
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
            registry = backend.registry,
            users = backend.users,
            friends = backend.friends
        )
    }

    private suspend fun tokenFor(users: UserRepository, login: String, name: String): AuthResult.Success =
        users.register(login, "password123", name) as AuthResult.Success

    @Test
    fun challengingAStrangerIsRejected() = testApplication {
        val backend = Backend()
        application(testModule(backend))
        val me = tokenFor(backend.users, "annalogin", "Anna")
        val other = tokenFor(backend.users, "borislogin", "Boris")

        val response = client().post("/api/challenges") {
            bearerAuth(me.token)
            contentType(ContentType.Application.Json)
            setBody(ChallengeRequest(other.profile.id))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(0, backend.registry.size)
    }

    @Test
    fun challengingAFriendCreatesARoom() = testApplication {
        val backend = Backend()
        application(testModule(backend))
        val me = tokenFor(backend.users, "annalogin", "Anna")
        val other = tokenFor(backend.users, "borislogin", "Boris")
        backend.friends.add(me.profile.id, "borislogin")

        val response = client().post("/api/challenges") {
            bearerAuth(me.token)
            contentType(ContentType.Application.Json)
            setBody(ChallengeRequest(other.profile.id))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val challenge = response.body<ChallengeResponse>()
        assertTrue(challenge.joinUrl.endsWith(challenge.shortCode))
        assertEquals(RoomStatus.WAITING_FOR_GUEST, backend.registry.byId(challenge.gameId)?.status)
    }
}
