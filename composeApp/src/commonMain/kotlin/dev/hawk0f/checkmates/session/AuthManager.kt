package dev.hawk0f.checkmates.session

import com.russhwolf.settings.Settings
import dev.hawk0f.checkmates.net.ApiClient
import dev.hawk0f.checkmates.net.configuredHttpClient
import dev.hawk0f.checkmates.shared.protocol.GameRecordRequest
import dev.hawk0f.checkmates.shared.protocol.ProfileResponse
import dev.hawk0f.checkmates.shared.protocol.UpdateProfileRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object AuthManager {

    private const val KEY_TOKEN = "auth.token"
    private const val KEY_PROFILE = "auth.profile"

    private val settings = Settings()
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val api = ApiClient(configuredHttpClient())

    private val _profile = MutableStateFlow(loadCachedProfile())
    val profile: StateFlow<ProfileResponse?> = _profile.asStateFlow()

    var token: String? = settings.getStringOrNull(KEY_TOKEN)
        private set

    val isLoggedIn: Boolean get() = token != null

    init {
        if (token != null) {
            scope.launch {
                runCatching { api.profile(token!!) }.onSuccess { fresh ->
                    storeProfile(fresh)
                }
            }
        }
    }

    suspend fun register(login: String, password: String, displayName: String): ProfileResponse {
        val response = api.register(login, password, displayName)
        storeAuth(response.token, response.profile)
        return response.profile
    }

    suspend fun login(login: String, password: String): ProfileResponse {
        val response = api.login(login, password)
        storeAuth(response.token, response.profile)
        return response.profile
    }

    suspend fun updateProfile(request: UpdateProfileRequest): ProfileResponse {
        val activeToken = token ?: error("not logged in")
        val fresh = api.updateProfile(activeToken, request)
        storeProfile(fresh)
        return fresh
    }

    fun logout() {
        val oldToken = token
        token = null
        _profile.value = null
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_PROFILE)
        if (oldToken != null) {
            scope.launch {
                runCatching { api.logout(oldToken) }
            }
        }
    }

    fun uploadGameIfLoggedIn(request: GameRecordRequest) {
        val activeToken = token ?: return
        scope.launch {
            runCatching { api.uploadGame(activeToken, request) }
        }
    }

    private fun storeAuth(newToken: String, profile: ProfileResponse) {
        token = newToken
        settings.putString(KEY_TOKEN, newToken)
        storeProfile(profile)
    }

    private fun storeProfile(profile: ProfileResponse) {
        _profile.value = profile
        settings.putString(KEY_PROFILE, json.encodeToString(ProfileResponse.serializer(), profile))
    }

    private fun loadCachedProfile(): ProfileResponse? =
        settings.getStringOrNull(KEY_PROFILE)?.let { cached ->
            runCatching { json.decodeFromString(ProfileResponse.serializer(), cached) }.getOrNull()
        }
}
