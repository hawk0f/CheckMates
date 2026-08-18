package dev.hawk0f.chess.ble

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

expect class BlePeripheralServer() {
    val incomingWrites: Flow<ByteArray>
    val centralConnected: StateFlow<Boolean>
    fun start(playerName: String, fenProvider: () -> String)
    suspend fun notifyGuest(bytes: ByteArray)
    fun stop()
}
