package dev.hawk0f.chess.ui.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Advertisement
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import dev.hawk0f.chess.ble.BleCentralTransport
import dev.hawk0f.chess.ble.BleConstants
import dev.hawk0f.chess.ble.BleHostEngine
import dev.hawk0f.chess.ble.BlePeripheralServer
import dev.hawk0f.chess.session.ActiveGameSession
import dev.hawk0f.chess.session.GameSessionHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface BleLobbyStep {
    data object Idle : BleLobbyStep
    data object Hosting : BleLobbyStep
    data object Scanning : BleLobbyStep
    data object Connecting : BleLobbyStep
    data object GameReady : BleLobbyStep
    data class Failed(val message: String) : BleLobbyStep
}

data class DiscoveredHost(
    val id: String,
    val label: String,
    val advertisement: Advertisement
)

data class BleLobbyUiState(
    val playerName: String = "",
    val step: BleLobbyStep = BleLobbyStep.Idle,
    val hosts: List<DiscoveredHost> = emptyList()
)

@OptIn(ExperimentalUuidApi::class)
class BleLobbyViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BleLobbyUiState())
    val uiState: StateFlow<BleLobbyUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var peripheralServer: BlePeripheralServer? = null

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(playerName = value.take(18))
    }

    fun startHosting() {
        stopScan()
        _uiState.value = _uiState.value.copy(step = BleLobbyStep.Hosting)
        viewModelScope.launch {
            try {
                val server = BlePeripheralServer()
                peripheralServer = server
                val engine = BleHostEngine(server, hostDisplayName())
                val session = ActiveGameSession(engine.localTransport, kind = "ble", myName = hostDisplayName())
                GameSessionHolder.install(session)
                engine.start()
                combine(session.myColor, session.opponentName) { color, name -> color != null && name != null }
                    .filter { it }
                    .first()
                _uiState.value = _uiState.value.copy(step = BleLobbyStep.GameReady)
            } catch (e: Exception) {
                stopHosting()
                _uiState.value = _uiState.value.copy(step = BleLobbyStep.Failed(e.message ?: "hosting failed"))
            }
        }
    }

    fun stopHosting() {
        peripheralServer?.stop()
        peripheralServer = null
        GameSessionHolder.clear()
        _uiState.value = _uiState.value.copy(step = BleLobbyStep.Idle)
    }

    fun startScan() {
        if (scanJob != null) {
            return
        }
        _uiState.value = _uiState.value.copy(step = BleLobbyStep.Scanning, hosts = emptyList())
        val scanner = Scanner {
            filters {
                match {
                    services = listOf(Uuid.parse(BleConstants.SERVICE_UUID))
                }
            }
        }
        scanJob = viewModelScope.launch {
            try {
                scanner.advertisements.collect { advertisement ->
                    val id = advertisement.identifier.toString()
                    val label = advertisement.peripheralName ?: "Chess host"
                    val hosts = _uiState.value.hosts
                    if (hosts.none { it.id == id }) {
                        _uiState.value = _uiState.value.copy(hosts = hosts + DiscoveredHost(id, label, advertisement))
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(step = BleLobbyStep.Failed(e.message ?: "scan failed"))
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    fun connectTo(host: DiscoveredHost) {
        stopScan()
        _uiState.value = _uiState.value.copy(step = BleLobbyStep.Connecting)
        viewModelScope.launch {
            try {
                val peripheral = Peripheral(host.advertisement)
                val transport = BleCentralTransport(
                    peripheral = peripheral,
                    guestName = _uiState.value.playerName.ifBlank { "Guest" }
                )
                val session = ActiveGameSession(transport, kind = "ble", myName = _uiState.value.playerName.ifBlank { "Guest" })
                GameSessionHolder.install(session)
                transport.connectAndJoin()
                combine(session.myColor, session.opponentName) { color, name -> color != null && name != null }
                    .filter { it }
                    .first()
                _uiState.value = _uiState.value.copy(step = BleLobbyStep.GameReady)
            } catch (e: Exception) {
                GameSessionHolder.clear()
                _uiState.value = _uiState.value.copy(step = BleLobbyStep.Failed(e.message ?: "connection failed"))
            }
        }
    }

    private fun hostDisplayName(): String = _uiState.value.playerName.ifBlank { "Host" }

    fun consumeGameReady() {
        _uiState.value = _uiState.value.copy(step = BleLobbyStep.Idle)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(step = BleLobbyStep.Idle)
    }

    override fun onCleared() {
        stopScan()
    }
}
