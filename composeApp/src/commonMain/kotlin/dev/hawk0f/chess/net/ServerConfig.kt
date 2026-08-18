package dev.hawk0f.chess.net

object ServerConfig {

    var baseUrl: String = "http://localhost:8080"

    val wsBaseUrl: String
        get() = baseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")

    fun wsGameUrl(gameId: String, token: String? = null): String =
        "$wsBaseUrl/ws/game/$gameId" + if (token != null) "?token=$token" else ""
}
