package dev.hawk0f.chess.shared.protocol

import dev.hawk0f.chess.shared.domain.GameOverReason
import dev.hawk0f.chess.shared.domain.PieceColor

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
            'D' -> GameMessage.OfferDraw
            'A' -> GameMessage.AcceptDraw
            'X' -> GameMessage.DeclineDraw
            'R' -> GameMessage.Resign
            'F' -> GameMessage.RequestResync
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
            'D' -> GameMessage.DrawOffered
            'X' -> GameMessage.DrawDeclined
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
        'd' -> GameOverReason.DISCONNECTION
        else -> null
    }

    private fun PieceColor?.toWireChar(): Char = when (this) {
        PieceColor.WHITE -> 'w'
        PieceColor.BLACK -> 'b'
        null -> 'n'
    }

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
