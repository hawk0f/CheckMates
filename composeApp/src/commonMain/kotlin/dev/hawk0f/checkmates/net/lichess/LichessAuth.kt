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
    const val SCOPE = "board:play"

    private const val KEY_TOKEN = "lichess.token"
    private const val KEY_USERNAME = "lichess.username"

    private val settings = Settings()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val api = LichessApi()

    private var codeVerifier: String? = null
    private var expectedState: String? = null

    private val _username = MutableStateFlow(settings.getStringOrNull(KEY_USERNAME))
    val username: StateFlow<String?> = _username.asStateFlow()

    var token: String? = settings.getStringOrNull(KEY_TOKEN)
        private set

    val isLoggedIn: Boolean get() = token != null

    fun buildAuthorizeUrl(): String {
        val verifier = base64UrlNoPadding(secureRandomBytes(48))
        val state = base64UrlNoPadding(secureRandomBytes(16))
        codeVerifier = verifier
        expectedState = state
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
        val verifier = codeVerifier ?: return Result.failure(LichessException("login was not started"))
        if (expectedState != null && state != expectedState) {
            return Result.failure(LichessException("state mismatch"))
        }
        return runCatching {
            val newToken = api.exchangeToken(code, verifier, REDIRECT_URI, CLIENT_ID)
            val account = api.account(newToken)
            token = newToken
            settings.putString(KEY_TOKEN, newToken)
            settings.putString(KEY_USERNAME, account.username)
            _username.value = account.username
            codeVerifier = null
            expectedState = null
            account.username
        }
    }

    suspend fun refreshUsername() {
        val activeToken = token ?: return
        runCatching { api.account(activeToken) }.onSuccess { account ->
            settings.putString(KEY_USERNAME, account.username)
            _username.value = account.username
        }
    }

    fun logout() {
        val oldToken = token
        token = null
        _username.value = null
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USERNAME)
        if (oldToken != null) {
            scope.launch {
                runCatching { api.revokeToken(oldToken) }
            }
        }
    }
}
