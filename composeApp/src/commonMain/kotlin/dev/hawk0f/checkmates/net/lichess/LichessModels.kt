package dev.hawk0f.checkmates.net.lichess

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LichessPerf(val key: String? = null, val name: String? = null)

@Serializable
data class LichessVariant(val key: String? = null, val name: String? = null)

@Serializable
data class LichessUserRef(
    val id: String? = null,
    val name: String? = null,
    val username: String? = null,
    val title: String? = null,
    val rating: Int? = null,
    val online: Boolean? = null,
    val playing: Boolean? = null,
    val playingId: String? = null
) {
    val label: String get() = username ?: name ?: id ?: "Anonymous"
}

@Serializable
data class LichessOpponent(
    val id: String? = null,
    val username: String? = null,
    val rating: Int? = null,
    val ai: Int? = null
) {
    val label: String get() = username ?: ai?.let { "Stockfish level $it" } ?: "Opponent"
}

@Serializable
data class LichessOngoingGame(
    val gameId: String,
    val fullId: String? = null,
    val color: String? = null,
    val fen: String? = null,
    val hasMoved: Boolean = false,
    val isMyTurn: Boolean = false,
    val lastMove: String? = null,
    val opponent: LichessOpponent? = null,
    val perf: String? = null,
    val rated: Boolean = false,
    val secondsLeft: Long? = null,
    val speed: String? = null,
    val source: String? = null,
    val variant: LichessVariant? = null
)

@Serializable
data class LichessOngoingGames(val nowPlaying: List<LichessOngoingGame> = emptyList())

@Serializable
data class LichessTimeControl(
    val type: String? = null,
    val limit: Int? = null,
    val increment: Int? = null,
    val show: String? = null,
    val daysPerTurn: Int? = null
) {
    val label: String get() = show ?: daysPerTurn?.let { "${it}d" } ?: type.orEmpty()
}

@Serializable
data class LichessChallenge(
    val id: String,
    val url: String? = null,
    val status: String? = null,
    val challenger: LichessUserRef? = null,
    val destUser: LichessUserRef? = null,
    val variant: LichessVariant? = null,
    val rated: Boolean = false,
    val speed: String? = null,
    val timeControl: LichessTimeControl? = null,
    val color: String? = null
)

@Serializable
data class LichessChallenges(
    @SerialName("in") val incoming: List<LichessChallenge> = emptyList(),
    @SerialName("out") val outgoing: List<LichessChallenge> = emptyList()
)

@Serializable
data class LichessOpenChallenge(
    val id: String,
    val url: String? = null,
    val urlWhite: String? = null,
    val urlBlack: String? = null
)

@Serializable
data class LichessPuzzleGame(
    val id: String? = null,
    val pgn: String = "",
    val clock: String? = null,
    val perf: LichessPerf? = null
)

@Serializable
data class LichessPuzzleBody(
    val id: String,
    val rating: Int? = null,
    val plays: Int? = null,
    val solution: List<String> = emptyList(),
    val themes: List<String> = emptyList(),
    val initialPly: Int = 0
)

@Serializable
data class LichessPuzzle(val game: LichessPuzzleGame, val puzzle: LichessPuzzleBody)

@Serializable
data class LichessPuzzleGlobal(
    val nb: Int = 0,
    val firstWins: Int = 0,
    val replayWins: Int = 0,
    val performance: Int = 0,
    val puzzleRatingAvg: Int = 0
)

@Serializable
data class LichessPuzzleDashboard(val days: Int = 0, val global: LichessPuzzleGlobal? = null)

@Serializable
data class LichessCloudPv(val moves: String = "", val cp: Int? = null, val mate: Int? = null)

@Serializable
data class LichessCloudEval(
    val fen: String = "",
    val knodes: Long = 0,
    val depth: Int = 0,
    val pvs: List<LichessCloudPv> = emptyList()
)

@Serializable
data class LichessAccuracy(val accuracy: Double? = null, val acpl: Int? = null)

@Serializable
data class LichessExportPlayer(
    val user: LichessUserRef? = null,
    val rating: Int? = null,
    val analysis: LichessAccuracy? = null
)

@Serializable
data class LichessExportPlayers(
    val white: LichessExportPlayer? = null,
    val black: LichessExportPlayer? = null
)

@Serializable
data class LichessGameExport(
    val id: String,
    val moves: String = "",
    val players: LichessExportPlayers? = null,
    val winner: String? = null,
    val status: String? = null,
    val speed: String? = null,
    val rated: Boolean = false
)

@Serializable
data class LichessOpening(val eco: String? = null, val name: String? = null)

@Serializable
data class LichessExplorerMove(
    val uci: String,
    val san: String? = null,
    val white: Int = 0,
    val draws: Int = 0,
    val black: Int = 0,
    val averageRating: Int? = null
) {
    val total: Int get() = white + draws + black
}

@Serializable
data class LichessExplorerGamePlayer(val name: String? = null, val rating: Int? = null)

@Serializable
data class LichessExplorerGame(
    val id: String? = null,
    val winner: String? = null,
    val white: LichessExplorerGamePlayer? = null,
    val black: LichessExplorerGamePlayer? = null,
    val year: Int? = null
)

@Serializable
data class LichessExplorerPosition(
    val white: Int = 0,
    val draws: Int = 0,
    val black: Int = 0,
    val moves: List<LichessExplorerMove> = emptyList(),
    val topGames: List<LichessExplorerGame> = emptyList(),
    val opening: LichessOpening? = null
) {
    val total: Int get() = white + draws + black
}

@Serializable
data class LichessTvChannel(
    val user: LichessUserRef? = null,
    val rating: Int? = null,
    val gameId: String? = null,
    val color: String? = null
)

@Serializable
data class LichessBroadcastRound(
    val id: String,
    val name: String? = null,
    val ongoing: Boolean = false,
    val finished: Boolean = false
)

@Serializable
data class LichessBroadcastTour(val id: String, val name: String? = null)

@Serializable
data class LichessBroadcast(
    val tour: LichessBroadcastTour,
    val rounds: List<LichessBroadcastRound> = emptyList()
)

@Serializable
data class LichessStreamerInfo(val name: String? = null, val headline: String? = null)

@Serializable
data class LichessStreamer(
    val id: String,
    val name: String? = null,
    val title: String? = null,
    val streamer: LichessStreamerInfo? = null
)

@Serializable
data class LichessClock(val limit: Int? = null, val increment: Int? = null)

@Serializable
data class LichessTournament(
    val id: String,
    val fullName: String? = null,
    val clock: LichessClock? = null,
    val nbPlayers: Int = 0,
    val minutes: Int? = null,
    val secondsToStart: Long? = null,
    val secondsToFinish: Long? = null,
    val rated: Boolean = false,
    val perf: LichessPerf? = null,
    val variant: String? = null,
    val status: Int? = null
)

@Serializable
data class LichessTournamentList(
    val created: List<LichessTournament> = emptyList(),
    val started: List<LichessTournament> = emptyList(),
    val finished: List<LichessTournament> = emptyList()
)

@Serializable
data class LichessSheet(val fire: Boolean = false)

@Serializable
data class LichessStandingPlayer(
    val name: String,
    val rank: Int = 0,
    val score: Int = 0,
    val sheet: LichessSheet? = null
)

@Serializable
data class LichessStanding(val players: List<LichessStandingPlayer> = emptyList())

@Serializable
data class LichessTournamentInfo(
    val id: String,
    val fullName: String? = null,
    val standing: LichessStanding? = null,
    val nbPlayers: Int = 0,
    val secondsToFinish: Long? = null,
    val isFinished: Boolean = false
)

@Serializable
data class LichessSwiss(
    val id: String,
    val name: String? = null,
    val nbRounds: Int = 0,
    val nbPlayers: Int = 0,
    val clock: LichessClock? = null,
    val rated: Boolean = true,
    val startsAt: String? = null
)

@Serializable
data class LichessTeam(val id: String, val name: String? = null, val nbMembers: Int = 0)

@Serializable
data class LichessLeaderboardUser(
    val id: String,
    val username: String,
    val title: String? = null,
    val perfs: Map<String, LichessRating> = emptyMap()
)

@Serializable
data class LichessRating(val rating: Int? = null, val progress: Int? = null)

@Serializable
data class LichessLeaderboard(val users: List<LichessLeaderboardUser> = emptyList())
