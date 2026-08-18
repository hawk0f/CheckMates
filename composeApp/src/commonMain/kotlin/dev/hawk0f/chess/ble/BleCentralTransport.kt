package dev.hawk0f.chess.ble

import com.juul.kable.Peripheral
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import dev.hawk0f.chess.shared.protocol.BleCodec
import dev.hawk0f.chess.shared.protocol.GameMessage
import dev.hawk0f.chess.shared.transport.GameTransport
import dev.hawk0f.chess.shared.transport.TransportConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BleCentralTransport(
    private val peripheral: Peripheral,
    private val guestName: String
) : GameTransport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val serviceUuid = Uuid.parse(BleConstants.SERVICE_UUID)
    private val moveToHost = characteristicOf(serviceUuid, Uuid.parse(BleConstants.CHAR_MOVE_TO_HOST))
    private val moveToGuest = characteristicOf(serviceUuid, Uuid.parse(BleConstants.CHAR_MOVE_TO_GUEST))
    private val fenChar = characteristicOf(serviceUuid, Uuid.parse(BleConstants.CHAR_FEN))

    private val _incoming = MutableSharedFlow<GameMessage>(extraBufferCapacity = 64)
    private val _connectionState = MutableStateFlow<TransportConnectionState>(TransportConnectionState.Connecting)

    override val incoming: Flow<GameMessage> = _incoming
    override val connectionState: StateFlow<TransportConnectionState> = _connectionState.asStateFlow()

    suspend fun connectAndJoin() {
        peripheral.connect()
        _connectionState.value = TransportConnectionState.Connected
        scope.launch {
            peripheral.observe(moveToGuest).collect { bytes ->
                BleCodec.decodeFromHost(bytes)?.let { _incoming.emit(it) }
            }
        }
        scope.launch {
            peripheral.state.collect { state ->
                if (state is State.Disconnected) {
                    _connectionState.value = TransportConnectionState.Closed("disconnected")
                }
            }
        }
        peripheral.write(moveToHost, BleCodec.encodeToHost(GameMessage.JoinGame("", guestName))!!, WriteType.WithResponse)
    }

    override suspend fun send(message: GameMessage) {
        if (message is GameMessage.RequestResync) {
            val fen = peripheral.read(fenChar).decodeToString()
            _incoming.emit(GameMessage.Resync(fen, emptyList(), drawOfferPending = false))
            return
        }
        BleCodec.encodeToHost(message)?.let { bytes ->
            peripheral.write(moveToHost, bytes, WriteType.WithResponse)
        }
    }

    override suspend fun close() {
        runCatching { peripheral.disconnect() }
        scope.cancel()
        _connectionState.value = TransportConnectionState.Closed(null)
    }
}
