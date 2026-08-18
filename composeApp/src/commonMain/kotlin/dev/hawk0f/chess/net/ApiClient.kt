package dev.hawk0f.chess.net

import dev.hawk0f.chess.shared.protocol.CreateGameRequest
import dev.hawk0f.chess.shared.protocol.CreateGameResponse
import dev.hawk0f.chess.shared.protocol.GameInfoResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ApiClient(private val client: HttpClient) {

    suspend fun createGame(hostName: String): CreateGameResponse =
        client.post("${ServerConfig.baseUrl}/api/games") {
            contentType(ContentType.Application.Json)
            setBody(CreateGameRequest(hostName))
        }.body()

    suspend fun gameInfo(code: String): GameInfoResponse =
        client.get("${ServerConfig.baseUrl}/api/games/$code").body()
}
