package dev.hawk0f.checkmates.shared.protocol

import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface GameMessage {

    @Serializable
    @SerialName("createGame")
    data class CreateGame(val playerName: String) : GameMessage

    @Serializable
    @SerialName("joinGame")
    data class JoinGame(val code: String, val playerName: String) : GameMessage

    @Serializable
    @SerialName("makeMove")
    data class MakeMove(val uci: String) : GameMessage

    @Serializable
    @SerialName("offerDraw")
    data object OfferDraw : GameMessage

    @Serializable
    @SerialName("acceptDraw")
    data object AcceptDraw : GameMessage

    @Serializable
    @SerialName("declineDraw")
    data object DeclineDraw : GameMessage

    @Serializable
    @SerialName("resign")
    data object Resign : GameMessage

    @Serializable
    @SerialName("offerTakeback")
    data object OfferTakeback : GameMessage

    @Serializable
    @SerialName("acceptTakeback")
    data object AcceptTakeback : GameMessage

    @Serializable
    @SerialName("declineTakeback")
    data object DeclineTakeback : GameMessage

    @Serializable
    @SerialName("takebackOffered")
    data object TakebackOffered : GameMessage

    @Serializable
    @SerialName("takebackDeclined")
    data object TakebackDeclined : GameMessage

    @Serializable
    @SerialName("takebackApplied")
    data class TakebackApplied(val plies: Int) : GameMessage

    @Serializable
    @SerialName("requestResync")
    data object RequestResync : GameMessage

    @Serializable
    @SerialName("reconnect")
    data class Reconnect(val gameId: String, val playerToken: String) : GameMessage

    @Serializable
    @SerialName("ping")
    data object Ping : GameMessage

    @Serializable
    @SerialName("claimTimeout")
    data object ClaimTimeout : GameMessage

    @Serializable
    @SerialName("registerPush")
    data class RegisterPush(val token: String, val platform: String = "android") : GameMessage

    @Serializable
    @SerialName("offerRematch")
    data object OfferRematch : GameMessage

    @Serializable
    @SerialName("acceptRematch")
    data object AcceptRematch : GameMessage

    @Serializable
    @SerialName("declineRematch")
    data object DeclineRematch : GameMessage

    @Serializable
    @SerialName("rematchOffered")
    data object RematchOffered : GameMessage

    @Serializable
    @SerialName("rematchDeclined")
    data object RematchDeclined : GameMessage

    @Serializable
    @SerialName("rematchStarted")
    data class RematchStarted(val color: PieceColor) : GameMessage

    @Serializable
    @SerialName("gameCreated")
    data class GameCreated(
        val gameId: String,
        val shortCode: String,
        val joinUrl: String,
        val playerToken: String
    ) : GameMessage

    @Serializable
    @SerialName("colorAssigned")
    data class ColorAssigned(val color: PieceColor) : GameMessage

    @Serializable
    @SerialName("opponentJoined")
    data class OpponentJoined(val opponentName: String) : GameMessage

    @Serializable
    @SerialName("moveApplied")
    data class MoveApplied(
        val uci: String,
        val fenAfter: String,
        val moveNumber: Int,
        val whiteMillis: Long? = null,
        val blackMillis: Long? = null
    ) : GameMessage

    @Serializable
    @SerialName("moveRejected")
    data class MoveRejected(val uci: String, val reason: String) : GameMessage

    @Serializable
    @SerialName("drawOffered")
    data object DrawOffered : GameMessage

    @Serializable
    @SerialName("drawDeclined")
    data object DrawDeclined : GameMessage

    @Serializable
    @SerialName("gameOver")
    data class GameOver(val reason: GameOverReason, val winner: PieceColor?) : GameMessage

    @Serializable
    @SerialName("resync")
    data class Resync(
        val fen: String,
        val uciHistory: List<String>,
        val drawOfferPending: Boolean,
        val timeControl: TimeControl? = null,
        val whiteMillis: Long? = null,
        val blackMillis: Long? = null
    ) : GameMessage

    @Serializable
    @SerialName("opponentConnectionChanged")
    data class OpponentConnectionChanged(val connected: Boolean) : GameMessage

    @Serializable
    @SerialName("pong")
    data object Pong : GameMessage

    @Serializable
    @SerialName("protocolError")
    data class ProtocolError(val code: String, val message: String) : GameMessage
}
