package dev.hawk0f.checkmates.net.lichess

import dev.hawk0f.checkmates.net.platformHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

const val LICHESS_BASE_URL = "https://lichess.org"

@Serializable
data class LichessAccount(val id: String, val username: String)

@Serializable
data class LichessTokenResponse(val access_token: String, val expires_in: Long? = null)

class LichessException(override val message: String) : Exception(message)

fun lichessHttpClient(): HttpClient = platformHttpClient().config {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = Long.MAX_VALUE
        socketTimeoutMillis = 120_000
    }
}

class LichessApi(private val client: HttpClient = lichessHttpClient()) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exchangeToken(
        code: String,
        codeVerifier: String,
        redirectUri: String,
        clientId: String
    ): String {
        val response: LichessTokenResponse = client.submitForm(
            url = "$LICHESS_BASE_URL/api/token",
            formParameters = parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("code_verifier", codeVerifier)
                append("redirect_uri", redirectUri)
                append("client_id", clientId)
            }
        ).body()
        return response.access_token
    }

    suspend fun account(token: String): LichessAccount =
        client.get("$LICHESS_BASE_URL/api/account") {
            bearerAuth(token)
        }.body()

    suspend fun revokeToken(token: String) {
        client.post("$LICHESS_BASE_URL/api/token") {
            bearerAuth(token)
        }
    }

    fun eventStream(token: String): Flow<JsonObject> =
        ndjson("$LICHESS_BASE_URL/api/stream/event", token)

    fun gameStream(token: String, gameId: String): Flow<JsonObject> =
        ndjson("$LICHESS_BASE_URL/api/board/game/stream/$gameId", token)

    suspend fun seek(token: String, minutes: Int, incrementSeconds: Int, rated: Boolean) {
        client.submitForm(
            url = "$LICHESS_BASE_URL/api/board/seek",
            formParameters = parameters {
                append("rated", rated.toString())
                append("time", minutes.toString())
                append("increment", incrementSeconds.toString())
                append("color", "random")
            }
        ) {
            bearerAuth(token)
            header(HttpHeaders.Accept, "application/x-ndjson")
        }
    }

    suspend fun challengeUser(
        token: String,
        username: String,
        clockLimitSeconds: Int,
        incrementSeconds: Int
    ): String {
        val body: JsonObject = client.submitForm(
            url = "$LICHESS_BASE_URL/api/challenge/$username",
            formParameters = parameters {
                append("rated", "false")
                append("clock.limit", clockLimitSeconds.toString())
                append("clock.increment", incrementSeconds.toString())
                append("color", "random")
                append("variant", "standard")
            }
        ) {
            bearerAuth(token)
        }.body()
        return body.stringAt("id") ?: body.objectAt("challenge")?.stringAt("id")
            ?: throw LichessException("challenge was not created")
    }

    suspend fun challengeAi(
        token: String,
        level: Int,
        clockLimitSeconds: Int,
        incrementSeconds: Int
    ): String {
        val body: JsonObject = client.submitForm(
            url = "$LICHESS_BASE_URL/api/challenge/ai",
            formParameters = parameters {
                append("level", level.toString())
                append("clock.limit", clockLimitSeconds.toString())
                append("clock.increment", incrementSeconds.toString())
                append("color", "random")
                append("variant", "standard")
            }
        ) {
            bearerAuth(token)
        }.body()
        return body.stringAt("id") ?: throw LichessException("game was not created")
    }

    suspend fun acceptChallenge(token: String, challengeId: String) {
        client.post("$LICHESS_BASE_URL/api/challenge/$challengeId/accept") {
            bearerAuth(token)
        }
    }

    suspend fun declineChallenge(token: String, challengeId: String) {
        client.post("$LICHESS_BASE_URL/api/challenge/$challengeId/decline") {
            bearerAuth(token)
        }
    }

    suspend fun cancelChallenge(token: String, challengeId: String) {
        client.post("$LICHESS_BASE_URL/api/challenge/$challengeId/cancel") {
            bearerAuth(token)
        }
    }

    suspend fun move(token: String, gameId: String, uci: String): Boolean =
        client.post("$LICHESS_BASE_URL/api/board/game/$gameId/move/$uci") {
            bearerAuth(token)
        }.status.isSuccess()

    suspend fun resign(token: String, gameId: String): Boolean =
        client.post("$LICHESS_BASE_URL/api/board/game/$gameId/resign") {
            bearerAuth(token)
        }.status.isSuccess()

    suspend fun abort(token: String, gameId: String): Boolean =
        client.post("$LICHESS_BASE_URL/api/board/game/$gameId/abort") {
            bearerAuth(token)
        }.status.isSuccess()

    suspend fun handleDraw(token: String, gameId: String, accept: Boolean): Boolean {
        val answer = if (accept) "yes" else "no"
        return client.post("$LICHESS_BASE_URL/api/board/game/$gameId/draw/$answer") {
            bearerAuth(token)
        }.status.isSuccess()
    }

    private fun ndjson(url: String, token: String): Flow<JsonObject> = flow {
        client.prepareGet(url) {
            bearerAuth(token)
            header(HttpHeaders.Accept, "application/x-ndjson")
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readLine() ?: break
                if (line.isBlank()) {
                    continue
                }
                val parsed = runCatching { json.parseToJsonElement(line) as? JsonObject }.getOrNull()
                if (parsed != null) {
                    emit(parsed)
                }
            }
        }
    }
}
