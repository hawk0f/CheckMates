package dev.hawk0f.checkmates.ble

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreBluetooth.CBATTErrorSuccess
import platform.CoreBluetooth.CBATTRequest
import platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey
import platform.CoreBluetooth.CBAttributePermissionsReadable
import platform.CoreBluetooth.CBAttributePermissionsWriteable
import platform.CoreBluetooth.CBCentral
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicPropertyNotify
import platform.CoreBluetooth.CBCharacteristicPropertyRead
import platform.CoreBluetooth.CBCharacteristicPropertyWrite
import platform.CoreBluetooth.CBCharacteristicPropertyWriteWithoutResponse
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBMutableCharacteristic
import platform.CoreBluetooth.CBMutableService
import platform.CoreBluetooth.CBPeripheralManager
import platform.CoreBluetooth.CBPeripheralManagerDelegateProtocol
import platform.CoreBluetooth.CBUUID
import kotlinx.cinterop.ObjCSignatureOverride
import platform.Foundation.NSData
import platform.Foundation.NSMakeRange
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding
import platform.Foundation.subdataWithRange
import platform.darwin.NSObject
import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class BlePeripheralServer {

    private val _incomingWrites = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    private val _centralConnected = MutableStateFlow(false)

    actual val incomingWrites: Flow<ByteArray> = _incomingWrites
    actual val centralConnected: StateFlow<Boolean> = _centralConnected.asStateFlow()

    private var manager: CBPeripheralManager? = null
    private var moveToGuestChar: CBMutableCharacteristic? = null
    private var playerName: String = ""
    private var fenProvider: () -> String = { "" }
    private val notifyQueue = ArrayDeque<ByteArray>()
    private var subscribed = false

    private val delegate = object : NSObject(), CBPeripheralManagerDelegateProtocol {

        override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
            if (peripheral.state == CBManagerStatePoweredOn) {
                setupService(peripheral)
            }
        }

        @ObjCSignatureOverride
        override fun peripheralManager(
            peripheral: CBPeripheralManager,
            central: CBCentral,
            didSubscribeToCharacteristic: CBCharacteristic
        ) {
            subscribed = true
            _centralConnected.value = true
            drainNotifyQueue()
        }

        @ObjCSignatureOverride
        override fun peripheralManager(
            peripheral: CBPeripheralManager,
            central: CBCentral,
            didUnsubscribeFromCharacteristic: CBCharacteristic
        ) {
            subscribed = false
            _centralConnected.value = false
        }

        override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveWriteRequests: List<*>) {
            for (request in didReceiveWriteRequests.filterIsInstance<CBATTRequest>()) {
                request.value?.toByteArray()?.let { _incomingWrites.tryEmit(it) }
                peripheral.respondToRequest(request, CBATTErrorSuccess)
            }
        }

        override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveReadRequest: CBATTRequest) {
            val uuid = didReceiveReadRequest.characteristic.UUID.UUIDString.lowercase()
            val payload = when (uuid) {
                BleConstants.CHAR_FEN.lowercase() -> fenProvider()
                BleConstants.CHAR_PLAYER_NAME.lowercase() -> playerName
                else -> ""
            }.toNSData()
            if (didReceiveReadRequest.offset.toInt() > payload.length.toInt()) {
                peripheral.respondToRequest(didReceiveReadRequest, platform.CoreBluetooth.CBATTErrorInvalidOffset)
                return
            }
            didReceiveReadRequest.value = payload.subdataWithRange(
                NSMakeRange(
                    didReceiveReadRequest.offset,
                    payload.length - didReceiveReadRequest.offset
                )
            )
            peripheral.respondToRequest(didReceiveReadRequest, CBATTErrorSuccess)
        }

        override fun peripheralManagerIsReadyToUpdateSubscribers(peripheral: CBPeripheralManager) {
            drainNotifyQueue()
        }
    }

    actual fun start(playerName: String, fenProvider: () -> String) {
        this.playerName = playerName
        this.fenProvider = fenProvider
        manager = CBPeripheralManager(delegate = delegate, queue = null)
    }

    private fun setupService(peripheral: CBPeripheralManager) {
        val service = CBMutableService(CBUUID.UUIDWithString(BleConstants.SERVICE_UUID), primary = true)
        val moveToHost = CBMutableCharacteristic(
            type = CBUUID.UUIDWithString(BleConstants.CHAR_MOVE_TO_HOST),
            properties = CBCharacteristicPropertyWrite or CBCharacteristicPropertyWriteWithoutResponse,
            value = null,
            permissions = CBAttributePermissionsWriteable
        )
        val moveToGuest = CBMutableCharacteristic(
            type = CBUUID.UUIDWithString(BleConstants.CHAR_MOVE_TO_GUEST),
            properties = CBCharacteristicPropertyNotify,
            value = null,
            permissions = CBAttributePermissionsReadable
        )
        val fen = CBMutableCharacteristic(
            type = CBUUID.UUIDWithString(BleConstants.CHAR_FEN),
            properties = CBCharacteristicPropertyRead,
            value = null,
            permissions = CBAttributePermissionsReadable
        )
        val name = CBMutableCharacteristic(
            type = CBUUID.UUIDWithString(BleConstants.CHAR_PLAYER_NAME),
            properties = CBCharacteristicPropertyRead,
            value = null,
            permissions = CBAttributePermissionsReadable
        )
        service.setCharacteristics(listOf(moveToHost, moveToGuest, fen, name))
        moveToGuestChar = moveToGuest
        peripheral.addService(service)
        peripheral.startAdvertising(
            mapOf<Any?, Any?>(
                CBAdvertisementDataServiceUUIDsKey to listOf(CBUUID.UUIDWithString(BleConstants.SERVICE_UUID))
            )
        )
    }

    actual suspend fun notifyGuest(bytes: ByteArray) {
        notifyQueue.addLast(bytes)
        drainNotifyQueue()
    }

    private fun drainNotifyQueue() {
        if (!subscribed) {
            return
        }
        val manager = manager ?: return
        val characteristic = moveToGuestChar ?: return
        while (notifyQueue.isNotEmpty()) {
            val bytes = notifyQueue.first()
            if (!manager.updateValue(bytes.toNSData(), characteristic, null)) {
                return
            }
            notifyQueue.removeFirst()
        }
    }

    actual fun stop() {
        manager?.stopAdvertising()
        manager?.removeAllServices()
        manager = null
        _centralConnected.value = false
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun String.toNSData(): NSData =
    (this as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: NSData()

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = if (isEmpty()) {
    NSData()
} else {
    usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) {
        return ByteArray(0)
    }
    val result = ByteArray(size)
    result.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), bytes, length)
    }
    return result
}
