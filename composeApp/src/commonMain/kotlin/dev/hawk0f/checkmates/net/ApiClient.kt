package dev.hawk0f.checkmates.net

import dev.hawk0f.checkmates.shared.protocol.ApiError
import dev.hawk0f.checkmates.shared.protocol.AuthResponse
import dev.hawk0f.checkmates.shared.protocol.AddFriendRequest
import dev.hawk0f.checkmates.shared.protocol.ChallengeRequest
import dev.hawk0f.checkmates.shared.protocol.ChallengeResponse
import dev.hawk0f.checkmates.shared.protocol.CrashReportRequest
import dev.hawk0f.checkmates.shared.protocol.FriendSummary
import dev.hawk0f.checkmates.shared.protocol.FriendsResponse
import dev.hawk0f.checkmates.shared.protocol.PushTokenRequest
import dev.hawk0f.checkmates.shared.protocol.CreateGameRequest
import dev.hawk0f.checkmates.shared.protocol.CreateGameResponse
import dev.hawk0f.checkmates.shared.protocol.GameHistoryResponse
import dev.hawk0f.checkmates.shared.protocol.GameInfoResponse
import dev.hawk0f.checkmates.shared.protocol.GameRecordRequest
import dev.hawk0f.checkmates.shared.protocol.GameRecordResponse
import dev.hawk0f.checkmates.shared.protocol.GameSpeed
import dev.hawk0f.checkmates.shared.protocol.LeaderboardResponse
import dev.hawk0f.checkmates.shared.protocol.LoginRequest
import dev.hawk0f.checkmates.shared.protocol.RatingsResponse
import dev.hawk0f.checkmates.shared.protocol.ProfileResponse
import dev.hawk0f.checkmates.shared.protocol.RegisterRequest
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import dev.hawk0f.checkmates.shared.protocol.UpdateProfileRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class ApiException(val code: String, override val message: String) : Exception(message)

private suspend inline fun <reified T> HttpResponse.bodyOrError(): T {
    if (!status.isSuccess()) {
        val error = runCatching { body<ApiError>() }.getOrNull()
        throw ApiException(error?.code ?: "HTTP_${status.value}", error?.message ?: status.toString())
    }
    return body()
}

class ApiClient(private val client: HttpClient) {

    suspend fun createGame(
        hostName: String,
        timeControl: TimeControl? = null,
        authToken: String? = null
    ): CreateGameResponse =
        client.post("${ServerConfig.baseUrl}/api/games") {
            authToken?.let { bearerAuth(it) }
            contentType(ContentType.Application.Json)
            setBody(CreateGameRequest(hostName, timeControl))
        }.body()

    suspend fun gameInfo(code: String): GameInfoResponse =
        client.get("${ServerConfig.baseUrl}/api/games/$code").body()

    suspend fun register(login: String, password: String, displayName: String): AuthResponse =
        client.post("${ServerConfig.baseUrl}/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(login, password, displayName))
        }.bodyOrError()

    suspend fun login(login: String, password: String): AuthResponse =
        client.post("${ServerConfig.baseUrl}/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(login, password))
        }.bodyOrError()

    suspend fun logout(token: String) {
        client.post("${ServerConfig.baseUrl}/api/auth/logout") {
            bearerAuth(token)
        }
    }

    suspend fun profile(token: String): ProfileResponse =
        client.get("${ServerConfig.baseUrl}/api/me") {
            bearerAuth(token)
        }.bodyOrError()

    suspend fun updateProfile(token: String, request: UpdateProfileRequest): ProfileResponse =
        client.patch("${ServerConfig.baseUrl}/api/me") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOrError()

    suspend fun gamesHistory(token: String): GameHistoryResponse =
        client.get("${ServerConfig.baseUrl}/api/me/games") {
            bearerAuth(token)
        }.bodyOrError()

    suspend fun friends(token: String): FriendsResponse =
        client.get("${ServerConfig.baseUrl}/api/me/friends") {
            bearerAuth(token)
        }.bodyOrError()

    suspend fun addFriend(token: String, displayName: String): FriendSummary =
        client.post("${ServerConfig.baseUrl}/api/me/friends") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(AddFriendRequest(displayName))
        }.bodyOrError()

    suspend fun removeFriend(token: String, friendUserId: Long) {
        client.delete("${ServerConfig.baseUrl}/api/me/friends/$friendUserId") {
            bearerAuth(token)
        }
    }

    suspend fun savePushToken(token: String, pushToken: String) {
        client.post("${ServerConfig.baseUrl}/api/me/push-token") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(PushTokenRequest(pushToken))
        }
    }

    suspend fun challenge(token: String, friendUserId: Long, timeControl: TimeControl?): ChallengeResponse =
        client.post("${ServerConfig.baseUrl}/api/challenges") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(ChallengeRequest(friendUserId, timeControl))
        }.bodyOrError()

    suspend fun reportCrash(request: CrashReportRequest) {
        val response = client.post("${ServerConfig.baseUrl}/api/crash") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw ApiException("HTTP_${response.status.value}", response.status.toString())
        }
    }

    suspend fun ratings(token: String): RatingsResponse =
        client.get("${ServerConfig.baseUrl}/api/me/ratings") {
            bearerAuth(token)
        }.bodyOrError()

    suspend fun leaderboard(speed: GameSpeed, limit: Int = 50): LeaderboardResponse =
        client.get("${ServerConfig.baseUrl}/api/leaderboard?speed=${speed.id}&limit=$limit").bodyOrError()

    suspend fun uploadGame(token: String, request: GameRecordRequest): GameRecordResponse =
        client.post("${ServerConfig.baseUrl}/api/me/games") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOrError()
}
