package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.protocol.GameRecordRequest

object GameRecordValidator {

    const val MAX_PLIES = 800

    val CLIENT_MODES = setOf("hotseat", "ble")

    private val replayableReasons = setOf(
        GameOverReason.CHECKMATE,
        GameOverReason.STALEMATE,
        GameOverReason.INSUFFICIENT_MATERIAL,
        GameOverReason.REPETITION,
        GameOverReason.FIFTY_MOVE
    )

    fun rejectionReason(request: GameRecordRequest): String? {
        if (request.mode !in CLIENT_MODES) {
            return "mode ${request.mode} is not accepted from clients"
        }
        if (request.uciHistory.size > MAX_PLIES) {
            return "history longer than $MAX_PLIES plies"
        }
        if (request.finishedAtMillis <= 0) {
            return "finishedAtMillis must be positive"
        }
        val game = ChessGame()
        for (uci in request.uciHistory) {
            if (game.applyUci(uci) is MoveOutcome.Illegal) {
                return "illegal move $uci in history"
            }
        }
        val replayed = game.state().result
        if (request.reason in replayableReasons) {
            if (replayed?.reason != request.reason || replayed.winner != request.winner) {
                return "declared ${request.reason} does not match the replayed position"
            }
            return null
        }
        if (replayed != null && replayed.winner != request.winner) {
            return "declared winner contradicts the final position"
        }
        return null
    }
}
