package com.arms.ke3

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ArmsBleManager {
    private lateinit var app: Context
    private val handler = Handler(Looper.getMainLooper())
    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null
    private val pendingWrites = ArrayDeque<ByteArray>()
    private var writing = false

    private val _status = MutableStateFlow("Non connesso")
    val status: StateFlow<String> = _status

    fun init(context: Context) { app = context.applicationContext }

    private fun hasBtPermission(): Boolean {
        return Build.VERSION.SDK_INT < 31 ||
            app.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun scanAndConnect() {
        if (!hasBtPermission()) { _status.value = "Permesso Bluetooth mancante"; return }
        val adapter = (app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val scanner = adapter.bluetoothLeScanner ?: run { _status.value = "Scanner BLE non disponibile"; return }
        _status.value = "Ricerca KE3 PRO..."
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.device.name ?: result.scanRecord?.deviceName ?: ""
                val address = result.device.address ?: ""
                if (name.contains("KE3", true) || address.equals("78:02:B7:09:08:7F", true)) {
                    scanner.stopScan(this)
                    connect(result.device)
                }
            }

            override fun onScanFailed(errorCode: Int) { _status.value = "Scan fallito: $errorCode" }
        }
        scanner.startScan(cb)
        handler.postDelayed({ try { scanner.stopScan(cb) } catch (_: Exception) {}; if (_status.value.startsWith("Ricerca")) _status.value = "KE3 PRO non trovato" }, 12000)
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        if (!hasBtPermission()) return
        _status.value = "Connessione ${device.name ?: device.address}..."
        gatt = device.connectGatt(app, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect(); gatt?.close(); gatt = null; writeChar = null; notifyChar = null; _status.value = "Disconnesso"
    }

    @SuppressLint("MissingPermission")
    fun sendNotification(text: String) {
        val ch = writeChar ?: run { _status.value = "Non pronto: 34F1 non trovato"; return }
        pendingWrites.clear()
        pendingWrites.addAll(Ke3Protocol.buildNotificationPackets(text))
        _status.value = "Invio notifica..."
        drainQueue(ch)
    }

    @SuppressLint("MissingPermission")
    private fun drainQueue(ch: BluetoothGattCharacteristic = writeChar ?: return) {
        if (writing || pendingWrites.isEmpty()) {
            if (pendingWrites.isEmpty()) _status.value = "Connesso - notifica inviata"
            return
        }
        val next = pendingWrites.removeFirst()
        writing = true
        if (Build.VERSION.SDK_INT >= 33) {
            gatt?.writeCharacteristic(ch, next, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            handler.postDelayed({ writing = false; drainQueue(ch) }, 70)
        } else {
            @Suppress("DEPRECATION")
            ch.value = next
            ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            gatt?.writeCharacteristic(ch)
            handler.postDelayed({ writing = false; drainQueue(ch) }, 70)
        }
    }

    @SuppressLint("MissingPermission")
    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _status.value = "Connesso, ricerca servizi..."
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _status.value = "Disconnesso"
                writeChar = null; notifyChar = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(Ke3Protocol.SERVICE_56FF)
            writeChar = service?.getCharacteristic(Ke3Protocol.CHAR_34F1_WRITE)
            notifyChar = service?.getCharacteristic(Ke3Protocol.CHAR_34F2_NOTIFY)
            notifyChar?.let {
                gatt.setCharacteristicNotification(it, true)
                it.getDescriptor(java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))?.let { d ->
                    if (Build.VERSION.SDK_INT >= 33) gatt.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    else { @Suppress("DEPRECATION") d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; @Suppress("DEPRECATION") gatt.writeDescriptor(d) }
                }
            }
            gatt.getService(Ke3Protocol.BATTERY_SERVICE)?.getCharacteristic(Ke3Protocol.BATTERY_LEVEL)?.let { gatt.readCharacteristic(it) }
            _status.value = if (writeChar != null) "Connesso - 34F1 pronto" else "Connesso ma 34F1 non trovato"
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            HealthRepository.updateRaw(Ke3Protocol.bytesToHex(value))
        }
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION") HealthRepository.updateRaw(Ke3Protocol.bytesToHex(characteristic.value ?: byteArrayOf()))
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (characteristic.uuid == Ke3Protocol.BATTERY_LEVEL && value.isNotEmpty()) HealthRepository.updateBattery(value[0].toInt() and 0xFF)
        }
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            @Suppress("DEPRECATION") if (characteristic.uuid == Ke3Protocol.BATTERY_LEVEL && characteristic.value != null && characteristic.value!!.isNotEmpty()) HealthRepository.updateBattery(characteristic.value!![0].toInt() and 0xFF)
        }
    }
}
