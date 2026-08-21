package dev.hawk0f.checkmates.shared.protocol

import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor

object BleCodec {

    const val MAX_MESSAGE_BYTES = 20

    fun encodeToHost(message: GameMessage): ByteArray? = when (message) {
        is GameMessage.MakeMove -> "M${message.uci}"
        is GameMessage.JoinGame -> "N${message.playerName.truncateToBytes(MAX_MESSAGE_BYTES - 1)}"
        GameMessage.OfferDraw -> "D"
        GameMessage.AcceptDraw -> "A"
        GameMessage.DeclineDraw -> "X"
        GameMessage.Resign -> "R"
        GameMessage.RequestResync -> "F"
        GameMessage.OfferTakeback -> "T"
        GameMessage.AcceptTakeback -> "Y"
        GameMessage.DeclineTakeback -> "Z"
        else -> null
    }?.encodeChecked()

    fun decodeFromGuest(bytes: ByteArray): GameMessage? {
        val text = bytes.decodeToString()
        if (text.isEmpty()) {
            return null
        }
        val payload = text.drop(1)
        return when (text[0]) {
            'M' -> GameMessage.MakeMove(payload)
            'N' -> GameMessage.JoinGame(code = "", playerName = payload)
            'D' -> GameMessage.OfferDraw.onlyIfBare(payload)
            'A' -> GameMessage.AcceptDraw.onlyIfBare(payload)
            'X' -> GameMessage.DeclineDraw.onlyIfBare(payload)
            'R' -> GameMessage.Resign.onlyIfBare(payload)
            'F' -> GameMessage.RequestResync.onlyIfBare(payload)
            'T' -> GameMessage.OfferTakeback.onlyIfBare(payload)
            'Y' -> GameMessage.AcceptTakeback.onlyIfBare(payload)
            'Z' -> GameMessage.DeclineTakeback.onlyIfBare(payload)
            else -> null
        }
    }

    fun encodeToGuest(message: GameMessage): ByteArray? = when (message) {
        is GameMessage.MoveApplied -> "M${message.uci}"
        is GameMessage.ColorAssigned -> "C${if (message.color == PieceColor.WHITE) 'w' else 'b'}"
        is GameMessage.OpponentJoined -> "N${message.opponentName.truncateToBytes(MAX_MESSAGE_BYTES - 1)}"
        is GameMessage.MoveRejected -> "!${message.uci.take(18)}"
        GameMessage.DrawOffered -> "D"
        GameMessage.DrawDeclined -> "X"
        GameMessage.TakebackOffered -> "T"
        GameMessage.TakebackDeclined -> "Z"
        is GameMessage.TakebackApplied -> "U${message.plies}"
        is GameMessage.GameOver -> "E${message.reason.toWireChar()}${message.winner.toWireChar()}"
        else -> null
    }?.encodeChecked()

    fun decodeFromHost(bytes: ByteArray): GameMessage? {
        val text = bytes.decodeToString()
        if (text.isEmpty()) {
            return null
        }
        val payload = text.drop(1)
        return when (text[0]) {
            'M' -> GameMessage.MoveApplied(payload, fenAfter = "", moveNumber = 0)
            'C' -> GameMessage.ColorAssigned(if (payload == "w") PieceColor.WHITE else PieceColor.BLACK)
            'N' -> GameMessage.OpponentJoined(payload)
            '!' -> GameMessage.MoveRejected(payload, "ILLEGAL")
            'D' -> GameMessage.DrawOffered.onlyIfBare(payload)
            'X' -> GameMessage.DrawDeclined.onlyIfBare(payload)
            'T' -> GameMessage.TakebackOffered.onlyIfBare(payload)
            'Z' -> GameMessage.TakebackDeclined.onlyIfBare(payload)
            'U' -> payload.toIntOrNull()?.takeIf { it > 0 }?.let { GameMessage.TakebackApplied(it) }
            'E' -> decodeGameOver(payload)
            else -> null
        }
    }

    private fun decodeGameOver(payload: String): GameMessage.GameOver? {
        if (payload.length != 2) {
            return null
        }
        val reason = payload[0].toReason() ?: return null
        val winner = when (payload[1]) {
            'w' -> PieceColor.WHITE
            'b' -> PieceColor.BLACK
            else -> null
        }
        return GameMessage.GameOver(reason, winner)
    }

    private fun GameOverReason.toWireChar(): Char = when (this) {
        GameOverReason.CHECKMATE -> 'c'
        GameOverReason.STALEMATE -> 's'
        GameOverReason.DRAW_AGREED -> 'g'
        GameOverReason.RESIGNATION -> 'r'
        GameOverReason.INSUFFICIENT_MATERIAL -> 'i'
        GameOverReason.REPETITION -> 'p'
        GameOverReason.FIFTY_MOVE -> 'f'
        GameOverReason.TIMEOUT -> 't'
        GameOverReason.DISCONNECTION -> 'd'
    }

    private fun Char.toReason(): GameOverReason? = when (this) {
        'c' -> GameOverReason.CHECKMATE
        's' -> GameOverReason.STALEMATE
        'g' -> GameOverReason.DRAW_AGREED
        'r' -> GameOverReason.RESIGNATION
        'i' -> GameOverReason.INSUFFICIENT_MATERIAL
        'p' -> GameOverReason.REPETITION
        'f' -> GameOverReason.FIFTY_MOVE
        't' -> GameOverReason.TIMEOUT
        'd' -> GameOverReason.DISCONNECTION
        else -> null
    }

    private fun PieceColor?.toWireChar(): Char = when (this) {
        PieceColor.WHITE -> 'w'
        PieceColor.BLACK -> 'b'
        null -> 'n'
    }

    private fun GameMessage.onlyIfBare(payload: String): GameMessage? =
        if (payload.isEmpty()) this else null

    private fun String.encodeChecked(): ByteArray {
        val bytes = encodeToByteArray()
        check(bytes.size <= MAX_MESSAGE_BYTES) { "BLE message too long: $this" }
        return bytes
    }

    private fun String.truncateToBytes(maxBytes: Int): String {
        var result = this
        while (result.encodeToByteArray().size > maxBytes) {
            result = result.dropLast(1)
        }
        return result
    }
}
