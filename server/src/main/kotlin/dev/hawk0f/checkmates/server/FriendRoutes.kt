package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.AddFriendRequest
import dev.hawk0f.checkmates.shared.protocol.ApiError
import dev.hawk0f.checkmates.shared.protocol.ChallengeRequest
import dev.hawk0f.checkmates.shared.protocol.ChallengeResponse
import dev.hawk0f.checkmates.shared.protocol.FriendsResponse
import dev.hawk0f.checkmates.shared.protocol.PushTokenRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.friendRoutes(
    users: UserRepository,
    friends: FriendRepository?,
    registry: RoomRegistry
) {
    if (friends == null) {
        return
    }

    get("/api/me/friends") {
        val userId = call.authenticatedUserId(users) ?: return@get
        call.respond(
            FriendsResponse(
                friends = friends.list(userId),
                recentOpponents = friends.recentOpponents(userId)
            )
        )
    }

    post("/api/me/friends") {
        val userId = call.authenticatedUserId(users) ?: return@post
        val request = call.receive<AddFriendRequest>()
        val added = friends.add(userId, request.query)
        if (added == null) {
            call.respond(HttpStatusCode.NotFound, ApiError("NO_PLAYER", "no player with that login or name"))
        } else {
            call.respond(added)
        }
    }

    delete("/api/me/friends/{id}") {
        val userId = call.authenticatedUserId(users) ?: return@delete
        val friendId = call.parameters["id"]?.toLongOrNull()
        if (friendId == null) {
            call.respond(HttpStatusCode.BadRequest, ApiError("BAD_ID", "friend id is not a number"))
            return@delete
        }
        friends.remove(userId, friendId)
        call.respond(HttpStatusCode.NoContent)
    }

    post("/api/me/push-token") {
        val userId = call.authenticatedUserId(users) ?: return@post
        val request = call.receive<PushTokenRequest>()
        if (request.token.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ApiError("EMPTY_TOKEN", "push token is empty"))
            return@post
        }
        friends.savePushToken(userId, request.token)
        call.respond(HttpStatusCode.NoContent)
    }

    rateLimit(RoomRateLimit) {
        post("/api/challenges") {
            val userId = call.authenticatedUserId(users) ?: return@post
            val request = call.receive<ChallengeRequest>()
            if (!friends.isFriend(userId, request.friendUserId)) {
                call.respond(HttpStatusCode.Forbidden, ApiError("NOT_A_FRIEND", "add this player as a friend first"))
                return@post
            }
            val myName = friends.displayNameOf(userId) ?: "Player"
            val created = registry.create(
                hostName = myName,
                timeControl = request.timeControl,
                hostUserId = userId
            )
            val joinUrl = registry.joinUrl(created.shortCode)
            val opponentToken = friends.pushTokenOf(request.friendUserId)
            val pushed = if (opponentToken != null) {
                FcmSender.send(
                    opponentToken,
                    "$myName challenges you",
                    "Join game ${created.shortCode}",
                    mapOf("shortCode" to created.shortCode)
                )
                true
            } else {
                false
            }
            call.respond(
                ChallengeResponse(
                    gameId = created.gameId,
                    shortCode = created.shortCode,
                    joinUrl = joinUrl,
                    playerToken = created.playerToken,
                    pushed = pushed
                )
            )
        }
    }
}
