package dev.hawk0f.chess.net

object ServerConfig {

    const val PRODUCTION_URL = "https://chess.hawk0f.icu"
    const val LOCAL_URL = "http://localhost:8080"

    var baseUrl: String = PRODUCTION_URL

    val wsBaseUrl: String
        get() = baseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")

    fun wsGameUrl(gameId: String, token: String? = null): String =
        "$wsBaseUrl/ws/game/$gameId" + if (token != null) "?token=$token" else ""
}
