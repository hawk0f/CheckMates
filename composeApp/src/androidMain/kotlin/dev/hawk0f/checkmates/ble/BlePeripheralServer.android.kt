package dev.hawk0f.checkmates.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object BleAppContext {
    lateinit var applicationContext: Context
}

@SuppressLint("MissingPermission")
actual class BlePeripheralServer {

    private val _incomingWrites = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    private val _centralConnected = MutableStateFlow(false)

    actual val incomingWrites: Flow<ByteArray> = _incomingWrites
    actual val centralConnected: StateFlow<Boolean> = _centralConnected.asStateFlow()

    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var moveToGuestChar: BluetoothGattCharacteristic? = null
    private var subscribedDevice: BluetoothDevice? = null
    private var playerName: String = ""
    private var fenProvider: () -> String = { "" }
    private val notifyQueue = ArrayDeque<ByteArray>()
    private var notifyInFlight = false

    private val advertiseCallback = object : AdvertiseCallback() {}

    actual fun start(playerName: String, fenProvider: () -> String) {
        this.playerName = playerName
        this.fenProvider = fenProvider
        val context = BleAppContext.applicationContext
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter ?: return

        val serverCallback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_DISCONNECTED && device == subscribedDevice) {
                    subscribedDevice = null
                    _centralConnected.value = false
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                if (characteristic.uuid == UUID.fromString(BleConstants.CHAR_MOVE_TO_HOST)) {
                    _incomingWrites.tryEmit(value)
                }
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                val payload = when (characteristic.uuid) {
                    UUID.fromString(BleConstants.CHAR_FEN) -> fenProvider().encodeToByteArray()
                    UUID.fromString(BleConstants.CHAR_PLAYER_NAME) -> this@BlePeripheralServer.playerName.encodeToByteArray()
                    else -> ByteArray(0)
                }
                val slice = if (offset <= payload.size) payload.copyOfRange(offset, payload.size) else ByteArray(0)
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, slice)
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
                if (descriptor.uuid == CCCD_UUID) {
                    subscribedDevice = device
                    _centralConnected.value = true
                    drainNotifyQueue()
                }
            }

            override fun onNotificationSent(device: BluetoothDevice, status: Int) {
                synchronized(notifyQueue) {
                    notifyInFlight = false
                }
                drainNotifyQueue()
            }
        }

        gattServer = manager.openGattServer(context, serverCallback)

        val service = BluetoothGattService(
            UUID.fromString(BleConstants.SERVICE_UUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        service.addCharacteristic(
            BluetoothGattCharacteristic(
                UUID.fromString(BleConstants.CHAR_MOVE_TO_HOST),
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
        )
        moveToGuestChar = BluetoothGattCharacteristic(
            UUID.fromString(BleConstants.CHAR_MOVE_TO_GUEST),
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            0
        ).also { characteristic ->
            characteristic.addDescriptor(
                BluetoothGattDescriptor(
                    CCCD_UUID,
                    BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
                )
            )
            service.addCharacteristic(characteristic)
        }
        service.addCharacteristic(
            BluetoothGattCharacteristic(
                UUID.fromString(BleConstants.CHAR_FEN),
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
        )
        service.addCharacteristic(
            BluetoothGattCharacteristic(
                UUID.fromString(BleConstants.CHAR_PLAYER_NAME),
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
        )
        gattServer?.addService(service)

        advertiser = adapter.bluetoothLeAdvertiser
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.fromString(BleConstants.SERVICE_UUID)))
            .setIncludeDeviceName(false)
            .build()
        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    actual suspend fun notifyGuest(bytes: ByteArray) {
        synchronized(notifyQueue) {
            notifyQueue.addLast(bytes)
        }
        drainNotifyQueue()
    }

    private fun drainNotifyQueue() {
        val device = subscribedDevice ?: return
        val characteristic = moveToGuestChar ?: return
        val server = gattServer ?: return
        val bytes = synchronized(notifyQueue) {
            if (notifyInFlight || notifyQueue.isEmpty()) {
                return
            }
            notifyInFlight = true
            notifyQueue.removeFirst()
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            server.notifyCharacteristicChanged(device, characteristic, false, bytes)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = bytes
            @Suppress("DEPRECATION")
            server.notifyCharacteristicChanged(device, characteristic, false)
        }
    }

    actual fun stop() {
        advertiser?.stopAdvertising(advertiseCallback)
        gattServer?.close()
        gattServer = null
        subscribedDevice = null
        _centralConnected.value = false
    }

    private companion object {
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
