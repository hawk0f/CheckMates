package dev.hawk0f.checkmates.net.lichess

import dev.hawk0f.checkmates.net.NO_IDLE_TIMEOUT_MILLIS
import dev.hawk0f.checkmates.net.platformHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.http.HttpStatusCode

const val LICHESS_BASE_URL = "https://lichess.org"
const val LICHESS_EXPLORER_URL = "https://explorer.lichess.org"

@Serializable
data class LichessAccount(
    val id: String,
    val username: String,
    val perfs: Map<String, LichessRating> = emptyMap()
)

@Serializable
data class LichessTokenResponse(val access_token: String, val expires_in: Long? = null)

class LichessException(override val message: String) : Exception(message)

fun lichessHttpClient(): HttpClient = platformHttpClient(NO_IDLE_TIMEOUT_MILLIS).config {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = Long.MAX_VALUE
        socketTimeoutMillis = 120_000
    }
    install(LichessThrottle)
    install(HttpRequestRetry) {
        maxRetries = 3
        retryIf { _, response ->
            response.status == HttpStatusCode.TooManyRequests || response.status.value >= 500
        }
        delayMillis { attempt ->
            val retryAfterSeconds = response?.headers?.get(HttpHeaders.RetryAfter)?.toLongOrNull()
            retryAfterSeconds?.times(1000)?.coerceAtMost(LichessRateLimit.DEFAULT_COOLDOWN_MILLIS)
                ?: (1000L * attempt)
        }
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

    suspend fun seek(
        token: String,
        minutes: Int,
        incrementSeconds: Int,
        rated: Boolean,
        variant: String = "standard",
        ratingRange: String? = null
    ) {
        client.submitForm(
            url = "$LICHESS_BASE_URL/api/board/seek",
            formParameters = parameters {
                append("rated", rated.toString())
                append("time", minutes.toString())
                append("increment", incrementSeconds.toString())
                append("color", "random")
                append("variant", variant)
                ratingRange?.let { append("ratingRange", it) }
            }
        ) {
            bearerAuth(token)
            header(HttpHeaders.Accept, "application/x-ndjson")
        }
    }

    suspend fun seekCorrespondence(token: String, days: Int, rated: Boolean): String? {
        val body: JsonObject = client.submitForm(
            url = "$LICHESS_BASE_URL/api/board/seek",
            formParameters = parameters {
                append("rated", rated.toString())
                append("days", days.toString())
                append("color", "random")
                append("variant", "standard")
            }
        ) {
            bearerAuth(token)
        }.body()
        return body.stringAt("id")
    }

    suspend fun challengeUser(
        token: String,
        username: String,
        clockLimitSeconds: Int,
        incrementSeconds: Int,
        rated: Boolean
    ): String {
        val body: JsonObject = client.submitForm(
            url = "$LICHESS_BASE_URL/api/challenge/$username",
            formParameters = parameters {
                append("rated", rated.toString())
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

    suspend fun ongoingGames(token: String): List<LichessOngoingGame> =
        client.get("$LICHESS_BASE_URL/api/account/playing") {
            bearerAuth(token)
            parameter("nb", 20)
        }.body<LichessOngoingGames>().nowPlaying

    suspend fun challenges(token: String): LichessChallenges =
        client.get("$LICHESS_BASE_URL/api/challenge") {
            bearerAuth(token)
        }.body()

    suspend fun openChallenge(
        token: String,
        clockLimitSeconds: Int,
        incrementSeconds: Int,
        rated: Boolean
    ): LichessOpenChallenge =
        client.submitForm(
            url = "$LICHESS_BASE_URL/api/challenge/open",
            formParameters = parameters {
                append("rated", rated.toString())
                append("clock.limit", clockLimitSeconds.toString())
                append("clock.increment", incrementSeconds.toString())
                append("variant", "standard")
            }
        ) {
            bearerAuth(token)
        }.body()

    suspend fun takeback(token: String, gameId: String, accept: Boolean): Boolean {
        val answer = if (accept) "yes" else "no"
        return client.post("$LICHESS_BASE_URL/api/board/game/$gameId/takeback/$answer") {
            bearerAuth(token)
        }.status.isSuccess()
    }

    suspend fun claimVictory(token: String, gameId: String): Boolean =
        client.post("$LICHESS_BASE_URL/api/board/game/$gameId/claim-victory") {
            bearerAuth(token)
        }.status.isSuccess()

    suspend fun chatHistory(token: String, gameId: String): List<LichessChatLine> = runCatching {
        val response = client.get("$LICHESS_BASE_URL/api/board/game/$gameId/chat") {
            bearerAuth(token)
        }
        if (!response.status.isSuccess()) {
            return emptyList()
        }
        response.body<JsonArray>().mapNotNull { element ->
            val entry = element as? JsonObject ?: return@mapNotNull null
            if (entry.stringAt("room") !in listOf(null, "player")) {
                return@mapNotNull null
            }
            val text = entry.stringAt("text")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            LichessChatLine(author = entry.stringAt("user") ?: "?", text = text)
        }
    }.getOrDefault(emptyList())

    suspend fun sendChat(token: String, gameId: String, text: String): Boolean =
        client.submitForm(
            url = "$LICHESS_BASE_URL/api/board/game/$gameId/chat",
            formParameters = parameters {
                append("room", "player")
                append("text", text)
            }
        ) {
            bearerAuth(token)
        }.status.isSuccess()

    suspend fun dailyPuzzle(): LichessPuzzle =
        client.get("$LICHESS_BASE_URL/api/puzzle/daily").body()

    suspend fun nextPuzzle(token: String?, angle: String?): LichessPuzzle =
        client.get("$LICHESS_BASE_URL/api/puzzle/next") {
            token?.let { bearerAuth(it) }
            angle?.let { parameter("angle", it) }
        }.body()

    suspend fun puzzleDashboard(token: String, days: Int): LichessPuzzleDashboard =
        client.get("$LICHESS_BASE_URL/api/puzzle/dashboard/$days") {
            bearerAuth(token)
        }.body()

    suspend fun cloudEval(fen: String, multiPv: Int): LichessCloudEval =
        client.get("$LICHESS_BASE_URL/api/cloud-eval") {
            parameter("fen", fen)
            parameter("multiPv", multiPv)
        }.body()

    suspend fun gameExport(gameId: String): LichessGameExport =
        client.get("$LICHESS_BASE_URL/game/export/$gameId") {
            header(HttpHeaders.Accept, "application/json")
            parameter("evals", true)
            parameter("accuracy", true)
            parameter("clocks", false)
        }.body()

    fun gamesOfUser(username: String, max: Int): Flow<JsonObject> =
        ndjson(
            "$LICHESS_BASE_URL/api/games/user/$username?max=$max&finished=true&sort=dateDesc",
            null
        )

    suspend fun explorerLichess(
        token: String,
        fen: String,
        speeds: List<String>,
        ratings: List<Int>
    ): LichessExplorerPosition =
        client.get("$LICHESS_EXPLORER_URL/lichess") {
            bearerAuth(token)
            parameter("fen", fen)
            parameter("speeds", speeds.joinToString(","))
            parameter("ratings", ratings.joinToString(","))
        }.body()

    suspend fun explorerMasters(token: String, fen: String): LichessExplorerPosition =
        client.get("$LICHESS_EXPLORER_URL/masters") {
            bearerAuth(token)
            parameter("fen", fen)
        }.body()

    suspend fun explorerPlayer(
        token: String,
        fen: String,
        player: String,
        color: String
    ): LichessExplorerPosition =
        client.get("$LICHESS_EXPLORER_URL/player") {
            bearerAuth(token)
            parameter("fen", fen)
            parameter("player", player)
            parameter("color", color)
        }.body()

    suspend fun tvChannels(): Map<String, LichessTvChannel> =
        client.get("$LICHESS_BASE_URL/api/tv/channels").body()

    fun tvFeed(): Flow<JsonObject> = ndjson("$LICHESS_BASE_URL/api/tv/feed", null)

    fun channelFeed(channel: String): Flow<JsonObject> =
        ndjson("$LICHESS_BASE_URL/api/tv/$channel/feed", null)

    fun broadcasts(count: Int): Flow<JsonObject> =
        ndjson("$LICHESS_BASE_URL/api/broadcast?nb=$count", null)

    suspend fun liveStreamers(): List<LichessStreamer> =
        client.get("$LICHESS_BASE_URL/api/streamer/live").body()

    suspend fun tournaments(): LichessTournamentList =
        client.get("$LICHESS_BASE_URL/api/tournament").body()

    suspend fun tournament(id: String): LichessTournamentInfo =
        client.get("$LICHESS_BASE_URL/api/tournament/$id").body()

    suspend fun joinTournament(token: String, id: String): Boolean =
        client.submitForm(
            url = "$LICHESS_BASE_URL/api/tournament/$id/join",
            formParameters = parameters { }
        ) {
            bearerAuth(token)
        }.status.isSuccess()

    suspend fun swiss(id: String): LichessSwiss =
        client.get("$LICHESS_BASE_URL/api/swiss/$id").body()

    suspend fun joinSwiss(token: String, id: String): Boolean =
        client.submitForm(
            url = "$LICHESS_BASE_URL/api/swiss/$id/join",
            formParameters = parameters { }
        ) {
            bearerAuth(token)
        }.status.isSuccess()

    suspend fun teamsOf(username: String): List<LichessTeam> =
        client.get("$LICHESS_BASE_URL/api/team/of/$username").body()

    fun following(token: String): Flow<JsonObject> =
        ndjson("$LICHESS_BASE_URL/api/rel/following", token)

    suspend fun usersStatus(ids: List<String>): List<LichessUserRef> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return client.get("$LICHESS_BASE_URL/api/users/status") {
            parameter("ids", ids.joinToString(","))
            parameter("withGameIds", true)
        }.body()
    }

    suspend fun leaderboard(count: Int, perf: String): LichessLeaderboard =
        client.get("$LICHESS_BASE_URL/api/player/top/$count/$perf").body()

    suspend fun autocompletePlayers(term: String): List<String> =
        client.get("$LICHESS_BASE_URL/api/player/autocomplete") {
            parameter("term", term)
        }.body()

    private fun ndjson(url: String, token: String?): Flow<JsonObject> = flow {
        client.prepareGet(url) {
            token?.let { bearerAuth(it) }
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
