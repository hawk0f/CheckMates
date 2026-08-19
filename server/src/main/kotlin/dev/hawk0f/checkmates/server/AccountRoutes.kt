package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.ApiError
import dev.hawk0f.checkmates.shared.protocol.GameHistoryResponse
import dev.hawk0f.checkmates.shared.protocol.GameRecordRequest
import dev.hawk0f.checkmates.shared.protocol.GameRecordResponse
import dev.hawk0f.checkmates.shared.protocol.LoginRequest
import dev.hawk0f.checkmates.shared.protocol.RegisterRequest
import dev.hawk0f.checkmates.shared.protocol.UpdateProfileRequest
import dev.hawk0f.checkmates.shared.protocol.AuthResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

fun Route.accountRoutes(users: UserRepository) {

    post("/api/auth/register") {
        val request = call.receive<RegisterRequest>()
        call.respondAuthResult(users.register(request.login, request.password, request.displayName))
    }

    post("/api/auth/login") {
        val request = call.receive<LoginRequest>()
        call.respondAuthResult(users.login(request.login, request.password))
    }

    post("/api/auth/logout") {
        val token = call.bearerToken()
        if (token != null) {
            users.logout(token)
        }
        call.respond(HttpStatusCode.NoContent)
    }

    get("/api/me") {
        val userId = call.authenticatedUserId(users) ?: return@get
        val profile = users.profile(userId)
        if (profile == null) {
            call.respond(HttpStatusCode.NotFound, ApiError("NO_PROFILE", "profile not found"))
        } else {
            call.respond(profile)
        }
    }

    patch("/api/me") {
        val userId = call.authenticatedUserId(users) ?: return@patch
        val request = call.receive<UpdateProfileRequest>()
        val profile = users.updateProfile(userId, request.displayName, request.avatarKind, request.avatarValue)
        if (profile == null) {
            call.respond(HttpStatusCode.NotFound, ApiError("NO_PROFILE", "profile not found"))
        } else {
            call.respond(profile)
        }
    }

    get("/api/me/games") {
        val userId = call.authenticatedUserId(users) ?: return@get
        call.respond(GameHistoryResponse(users.listGames(userId)))
    }

    post("/api/me/games") {
        val userId = call.authenticatedUserId(users) ?: return@post
        val request = call.receive<GameRecordRequest>()
        val id = users.insertGame(userId, request)
        call.respond(GameRecordResponse(id))
    }
}

private suspend fun ApplicationCall.respondAuthResult(result: AuthResult) {
    when (result) {
        is AuthResult.Success -> respond(AuthResponse(result.token, result.profile))
        is AuthResult.Failure -> respond(HttpStatusCode.BadRequest, ApiError(result.code, result.message))
    }
}

fun ApplicationCall.bearerToken(): String? =
    request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()?.takeIf { it.isNotEmpty() }

suspend fun ApplicationCall.authenticatedUserId(users: UserRepository): Long? {
    val token = bearerToken()
    if (token == null) {
        respond(HttpStatusCode.Unauthorized, ApiError("NO_TOKEN", "missing bearer token"))
        return null
    }
    val userId = users.userIdByToken(token)
    if (userId == null) {
        respond(HttpStatusCode.Unauthorized, ApiError("BAD_TOKEN", "invalid or expired token"))
        return null
    }
    return userId
}
