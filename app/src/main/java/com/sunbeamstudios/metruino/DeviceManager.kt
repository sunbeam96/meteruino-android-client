@file:Suppress("DEPRECATION")

package com.sunbeamstudios.metruino

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import java.nio.ByteOrder
import java.util.UUID
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction2


class DeviceManager {
    private val TAG = "DeviceManager"
    private var foundDevices = arrayListOf<BluetoothDevice>()
    private var currentMetruino: BluetoothDevice? = null
    var bluetoothGatt: BluetoothGatt? = null
    private var scanning = false
    private val handler = Handler()
    private val SCAN_PERIOD: Long = 10000
    private lateinit var updaterCallback: (BluetoothGattCharacteristic, ByteArray) -> Unit
    private lateinit var connectionCallback: () -> Unit
    private lateinit var measurementStartCallback: () -> Unit
    val meterService: UUID = UUID.fromString("00002000-0000-1000-8000-00805f9b34fb")
    var characteristicsToReadList: List<BluetoothGattCharacteristic> = listOf()
    private var connected = false

    @SuppressLint("MissingPermission")
    fun addDevice(device: BluetoothDevice?) {
        if (foundDevices?.contains(device!!) != true) {
            if (device != null && device.name != null) {
                Log.d(TAG, "Found device: " + device.name)
                foundDevices?.add(device)
                }
        }
    }

    public fun registerUpdaterCallback(newCallback: KFunction2<BluetoothGattCharacteristic, ByteArray, Unit>){
        updaterCallback = newCallback
    }

    public fun registerConnectionCallback(newCallback: KFunction0<Unit>){
        connectionCallback = newCallback
    }

    @SuppressLint("MissingPermission")
    fun connectToMetruinoGatt(context: Context) {
        Log.i(TAG, "Attempting GATT connection")
        bluetoothGatt = currentMetruino?.connectGatt(context, false, bluetoothGattCallback)
        bluetoothGatt?.requestConnectionPriority(CONNECTION_PRIORITY_HIGH)
        bluetoothGatt?.requestMtu(50)
    }

    @SuppressLint("MissingPermission")
    private fun findMetruinoFromScanned() {
        if (foundDevices != null) {
            for (device in foundDevices){
                if (device.name.contains("Telemetruino-A563221")){
                    currentMetruino = device
                    Log.i(TAG, "Found matching device - Telemetruino-A563221")
                    return
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun scanLeDevices(scanner: BluetoothLeScanner) {
        Log.d(TAG, "scanLeDevices run")
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                scanner.stopScan(leScanCallback)
                findMetruinoFromScanned()
                connectionCallback()
            }, SCAN_PERIOD)
            scanning = true
            scanner.startScan(leScanCallback)
        } else {
            scanning = false
            scanner.stopScan(leScanCallback)
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            addDevice(result.device)
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Device state changed to connected")
                connected = true
                bluetoothGatt?.discoverServices()
                // successfully connected to the GATT Server
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connected = false
                Log.i(TAG, "Device state changed to disconnected. Attempting connection.")
                gatt?.connect()
                gatt?.requestConnectionPriority(CONNECTION_PRIORITY_HIGH)
                gatt?.requestMtu(50)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (service in bluetoothGatt?.services!!){
                    // should only check characteristics from the valid BLE service
                    if (service.uuid != meterService)
                        continue
                    characteristicsToReadList = service.characteristics
                    readCharacteristic(characteristicsToReadList[0])
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        @Suppress("DEPRECATION")
        @Deprecated(
            "Used natively in Android 12 and lower",
            ReplaceWith("onCharacteristicRead(gatt, characteristic, characteristic.value, status)")
        )
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) = onCharacteristicRead(gatt, characteristic, characteristic.value, status)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (value?.isNotEmpty() == true) {
                    updaterCallback(characteristic, value)
                }
                else{
                    Log.w(TAG, "Empty characteristic value. Something wrong with device.")
                }
                if (connected)
                    readCharacteristic(characteristicsToReadList[0])
            }
            else{
                connected = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            var myThread = Thread(Runnable(){
                gatt.readCharacteristic(characteristic)
            })
            myThread.start()
            } ?: run {
            Log.d(TAG, "bluetoothGatt is not available")
            return
        }
    }
}
