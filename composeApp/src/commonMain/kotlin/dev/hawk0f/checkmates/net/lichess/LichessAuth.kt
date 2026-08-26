package dev.hawk0f.checkmates.net.lichess

import com.russhwolf.settings.Settings
import dev.hawk0f.checkmates.platform.secureRandomBytes
import dev.hawk0f.checkmates.shared.util.base64UrlNoPadding
import dev.hawk0f.checkmates.shared.util.sha256
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object LichessAuth {

    const val CLIENT_ID = "dev.hawk0f.checkmates"
    const val REDIRECT_URI = "dev.hawk0f.checkmates://lichess-auth"
    const val SCOPE =
        "board:play challenge:read challenge:write puzzle:read tournament:write follow:read"

    val PERMISSIONS_GRANTED = listOf(
        "Play your games" to "Make moves, offer draws, resign",
        "Send and accept invites" to "Challenge the people you follow",
        "Read your puzzle history" to "Rating, streak, solved themes",
        "Join tournaments" to "Arenas and Swiss events",
        "See who you follow" to "Online status of your list"
    )

    val PERMISSIONS_DECLINED = listOf("Your email address" to "Never requested")

    private const val KEY_TOKEN = "lichess.token"
    private const val KEY_USERNAME = "lichess.username"
    private const val KEY_VERIFIER = "lichess.verifier"
    private const val KEY_STATE = "lichess.state"

    private val settings: Settings? by lazy { runCatching { Settings() }.getOrNull() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val api = LichessApi()

    private val _username = MutableStateFlow(settings?.getStringOrNull(KEY_USERNAME))
    val username: StateFlow<String?> = _username.asStateFlow()

    var token: String? = settings?.getStringOrNull(KEY_TOKEN)
        private set

    val isLoggedIn: Boolean get() = token != null

    fun buildAuthorizeUrl(): String {
        val verifier = base64UrlNoPadding(secureRandomBytes(48))
        val state = base64UrlNoPadding(secureRandomBytes(16))
        settings?.putString(KEY_VERIFIER, verifier)
        settings?.putString(KEY_STATE, state)
        val challenge = base64UrlNoPadding(sha256(verifier.encodeToByteArray()))
        return "$LICHESS_BASE_URL/oauth" +
            "?response_type=code" +
            "&client_id=${CLIENT_ID.encodeURLParameter()}" +
            "&redirect_uri=${REDIRECT_URI.encodeURLParameter()}" +
            "&scope=${SCOPE.encodeURLParameter()}" +
            "&code_challenge_method=S256" +
            "&code_challenge=$challenge" +
            "&state=$state"
    }

    suspend fun completeLogin(code: String, state: String?): Result<String> {
        val verifier = settings?.getStringOrNull(KEY_VERIFIER)
            ?: return Result.failure(LichessException("login was not started"))
        val expectedState = settings?.getStringOrNull(KEY_STATE)
        if (expectedState != null && state != expectedState) {
            return Result.failure(LichessException("state mismatch"))
        }
        return runCatching {
            val newToken = api.exchangeToken(code, verifier, REDIRECT_URI, CLIENT_ID)
            val account = api.account(newToken)
            token = newToken
            settings?.putString(KEY_TOKEN, newToken)
            settings?.putString(KEY_USERNAME, account.username)
            _username.value = account.username
            settings?.remove(KEY_VERIFIER)
            settings?.remove(KEY_STATE)
            account.username
        }
    }

    suspend fun refreshUsername() {
        val activeToken = token ?: return
        runCatching { api.account(activeToken) }.onSuccess { account ->
            settings?.putString(KEY_USERNAME, account.username)
            _username.value = account.username
        }
    }

    fun logout() {
        val oldToken = token
        token = null
        _username.value = null
        settings?.remove(KEY_TOKEN)
        settings?.remove(KEY_USERNAME)
        if (oldToken != null) {
            scope.launch {
                runCatching { api.revokeToken(oldToken) }
            }
        }
    }
}
