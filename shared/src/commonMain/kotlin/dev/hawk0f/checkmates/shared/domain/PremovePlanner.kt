package dev.hawk0f.checkmates.shared.domain

object PremovePlanner {

    const val MAX_PREMOVES = 6

    fun project(fen: String, premoves: List<String>): ChessGame? {
        var game = ChessGame()
        if (runCatching { game.loadFen(fen) }.isFailure) {
            return null
        }
        for (uci in premoves) {
            if (game.applyUci(uci) is MoveOutcome.Illegal) {
                return null
            }
            val flipped = flipSideToMove(game.fen()) ?: return null
            val next = ChessGame()
            if (runCatching { next.loadFen(flipped) }.isFailure) {
                return null
            }
            game = next
        }
        return game
    }

    fun planningFen(liveFen: String): String? = flipSideToMove(liveFen)

    fun canAppend(fen: String, premoves: List<String>, uci: String): Boolean {
        if (premoves.size >= MAX_PREMOVES) {
            return false
        }
        return project(fen, premoves + uci) != null
    }

    fun isPlayableNow(game: ChessGame, uci: String): Boolean {
        if (uci.length < 4) {
            return false
        }
        val from = runCatching { Square.fromUci(uci.substring(0, 2)) }.getOrNull() ?: return false
        val to = runCatching { Square.fromUci(uci.substring(2, 4)) }.getOrNull() ?: return false
        if (to !in game.legalDestinations(from)) {
            return false
        }
        return game.isPromotionMove(from, to) == (uci.length == 5)
    }

    fun flipSideToMove(fen: String): String? {
        val fields = fen.split(" ")
        if (fields.size < 4) {
            return null
        }
        val side = when (fields[1]) {
            "w" -> "b"
            "b" -> "w"
            else -> return null
        }
        return (listOf(fields[0], side, fields[2], "-") + fields.drop(4)).joinToString(" ")
    }
}
