package dev.hawk0f.checkmates.net

import dev.hawk0f.checkmates.shared.protocol.ApiError
import dev.hawk0f.checkmates.shared.protocol.AuthResponse
import dev.hawk0f.checkmates.shared.protocol.CreateGameRequest
import dev.hawk0f.checkmates.shared.protocol.CreateGameResponse
import dev.hawk0f.checkmates.shared.protocol.GameHistoryResponse
import dev.hawk0f.checkmates.shared.protocol.GameInfoResponse
import dev.hawk0f.checkmates.shared.protocol.GameRecordRequest
import dev.hawk0f.checkmates.shared.protocol.GameRecordResponse
import dev.hawk0f.checkmates.shared.protocol.LoginRequest
import dev.hawk0f.checkmates.shared.protocol.ProfileResponse
import dev.hawk0f.checkmates.shared.protocol.RegisterRequest
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import dev.hawk0f.checkmates.shared.protocol.UpdateProfileRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
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

    suspend fun uploadGame(token: String, request: GameRecordRequest): GameRecordResponse =
        client.post("${ServerConfig.baseUrl}/api/me/games") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOrError()
}
