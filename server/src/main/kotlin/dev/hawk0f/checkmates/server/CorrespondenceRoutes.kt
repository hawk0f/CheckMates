package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.ApiError
import dev.hawk0f.checkmates.shared.protocol.CorrespondenceGamesResponse
import dev.hawk0f.checkmates.shared.protocol.CorrespondenceMoveRequest
import dev.hawk0f.checkmates.shared.protocol.CreateCorrespondenceRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.correspondenceRoutes(
    users: UserRepository,
    games: CorrespondenceRepository,
    friends: FriendRepository
) {
    get("/api/correspondence") {
        val userId = call.authenticatedUserId(users) ?: return@get
        call.respond(CorrespondenceGamesResponse(games.list(userId)))
    }

    post("/api/correspondence") {
        val userId = call.authenticatedUserId(users) ?: return@post
        val request = call.receive<CreateCorrespondenceRequest>()
        if (request.opponentUserId == userId) {
            call.respond(HttpStatusCode.BadRequest, ApiError("SELF_GAME", "choose another player"))
            return@post
        }
        if (!friends.isFriend(userId, request.opponentUserId)) {
            call.respond(HttpStatusCode.Forbidden, ApiError("NOT_A_FRIEND", "add this player as a friend first"))
            return@post
        }
        val game = games.create(userId, request.opponentUserId)
        if (game == null) {
            call.respond(HttpStatusCode.NotFound, ApiError("NO_PLAYER", "player not found"))
            return@post
        }
        friends.pushTokenOf(request.opponentUserId)?.let { token ->
            FcmSender.send(token, "${game.whiteName} started a correspondence game", "It is your turn after White moves")
        }
        call.respond(game)
    }

    post("/api/correspondence/{id}/moves") {
        val userId = call.authenticatedUserId(users) ?: return@post
        val gameId = call.parameters["id"]?.toLongOrNull()
        if (gameId == null) {
            call.respond(HttpStatusCode.BadRequest, ApiError("BAD_ID", "game id is not a number"))
            return@post
        }
        val request = call.receive<CorrespondenceMoveRequest>()
        when (val result = games.move(gameId, userId, request.uci)) {
            CorrespondenceMoveResult.NotFound -> call.respond(HttpStatusCode.NotFound, ApiError("NO_GAME", "game not found"))
            CorrespondenceMoveResult.NotYourTurn -> call.respond(
                HttpStatusCode.Conflict,
                ApiError("NOT_YOUR_TURN", "wait for your opponent")
            )
            CorrespondenceMoveResult.IllegalMove -> call.respond(HttpStatusCode.BadRequest, ApiError("ILLEGAL_MOVE", "illegal move"))
            CorrespondenceMoveResult.Finished -> call.respond(HttpStatusCode.Conflict, ApiError("GAME_OVER", "game is finished"))
            is CorrespondenceMoveResult.Success -> {
                friends.pushTokenOf(result.opponentUserId)?.let { token ->
                    FcmSender.send(token, "Correspondence move", "It is your turn", mapOf("correspondenceId" to gameId.toString()))
                }
                call.respond(result.game)
            }
        }
    }
}
