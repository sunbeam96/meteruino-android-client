@file:Suppress("DEPRECATION")

package com.sunbeamstudios.metruino

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val deviceManager = DeviceManager()
    //
    val speedCharUuid = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    val accelXCharUuid = UUID.fromString("0000aa10-0000-1000-8000-00805f9b34fb")
    val accelYCharUuid = UUID.fromString("00002b3f-0000-1000-8000-00805f9b34fb")
    val accelZCharUuid = UUID.fromString("0000183e-0000-1000-8000-00805f9b34fb")
    val gyroXCharUuid = UUID.fromString("0000183a-0000-1000-8000-00805f9b34fb")
    val gyroYCharUuid = UUID.fromString("00002a2f-0000-1000-8000-00805f9b34fb")
    val gyroZCharUuid = UUID.fromString("00001a3b-0000-1000-8000-00805f9b34fb")
    var speedValsEntryList = ArrayList<Entry>()
    var gyroXValsEntryList = ArrayList<Entry>()
    var gyroYValsEntryList = ArrayList<Entry>()
    var gyroZValsEntryList = ArrayList<Entry>()
    var accelXValsEntryList = ArrayList<Entry>()
    var accelYValsEntryList = ArrayList<Entry>()
    var accelZValsEntryList = ArrayList<Entry>()
    val startTime: Long = System.currentTimeMillis()

    enum class Axis{
        X, Y, Z
    }
    enum class MeasType{
        Speed, Accelerometer, Gyroscope
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate run")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "Requesting permissions")
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        var REQUEST_ENABLE_BT = 0
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                0
            )
        }
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                0
            )
        }
    }

    private fun initializeBluetoothObjects(){
        Log.d(TAG, "Bluetooth manager and adapter initialization")
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter!!
        if (!bluetoothAdapter.isEnabled) {
            Log.d(TAG, "Bluetooth adapter not enabled")
        }
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    }

    private fun getMeasurementTime(): Float{
        val measurementTimeDifference = System.currentTimeMillis() - startTime
        return 0.001F * measurementTimeDifference
    }

    private fun onMeasurementUpdate(lineData: LineData, measurementType: MeasType){
        if (measurementType == MeasType.Speed){
            val speedChart = findViewById<View>(R.id.chartSpeed) as LineChart
            speedChart.data = lineData
            speedChart.invalidate()
        }
        if (measurementType == MeasType.Accelerometer){
            val accelChart = findViewById<View>(R.id.chartAccelerometer) as LineChart
            accelChart.data = lineData
            accelChart.invalidate()
        }
        if (measurementType == MeasType.Gyroscope){
            val gyroChart = findViewById<View>(R.id.chartGyroscope) as LineChart
            gyroChart.data = lineData
            gyroChart.invalidate()
        }
    }

    private fun afterScanCallback(){
        deviceManager.connectToMetruinoGatt(this)
    }

    private fun byteArrayToFloat(inputByteArray: ByteArray): Float{
        if (inputByteArray.size == 1)
            return inputByteArray[0].toFloat()
        return ByteBuffer.wrap(inputByteArray).order(ByteOrder.LITTLE_ENDIAN).float
    }

    private fun updateGyroChart(gyroData: ByteArray, axis: Axis){
        var dataEntry = Entry(getMeasurementTime(), byteArrayToFloat(gyroData))
        val lineData = LineData()
        if (axis == Axis.X){
            gyroXValsEntryList.add(dataEntry)
        }
        if (axis == Axis.Y){
            gyroYValsEntryList.add(dataEntry)
        }
        if (axis == Axis.Z){
            gyroZValsEntryList.add(dataEntry)
        }
        val dataSetX: LineDataSet = LineDataSet(gyroXValsEntryList, "Gyro X")
        dataSetX.color = Color.RED
        dataSetX.valueTextColor = Color.BLUE
        lineData.addDataSet(dataSetX)

        val dataSetY: LineDataSet = LineDataSet(gyroYValsEntryList, "Gyro Y")
        dataSetY.color = Color.GREEN
        dataSetY.valueTextColor = Color.YELLOW
        lineData.addDataSet(dataSetY)

        val dataSetZ: LineDataSet = LineDataSet(gyroZValsEntryList, "Gyro Z")
        dataSetZ.color = Color.CYAN
        dataSetZ.valueTextColor = Color.GRAY
        lineData.addDataSet(dataSetZ)

        onMeasurementUpdate(lineData, MeasType.Gyroscope)
    }

    private fun updateAccelChart(accelData: ByteArray, axis: Axis){
        var dataEntry = Entry(getMeasurementTime(), byteArrayToFloat(accelData))
        val lineData = LineData()
        if (axis == Axis.X){
            accelXValsEntryList.add(dataEntry)
        }
        if (axis == Axis.Y){
            accelYValsEntryList.add(dataEntry)
        }
        if (axis == Axis.Z){
            accelZValsEntryList.add(dataEntry)
        }
        val dataSetX: LineDataSet = LineDataSet(accelXValsEntryList, "Accel X")
        dataSetX.color = Color.RED
        dataSetX.valueTextColor = Color.BLUE
        lineData.addDataSet(dataSetX)

        val dataSetY: LineDataSet = LineDataSet(accelYValsEntryList, "Accel Y")
        dataSetY.color = Color.GREEN
        dataSetY.valueTextColor = Color.YELLOW
        lineData.addDataSet(dataSetY)

        val dataSetZ: LineDataSet = LineDataSet(accelZValsEntryList, "Accel Z")
        dataSetZ.color = Color.CYAN
        dataSetZ.valueTextColor = Color.GRAY
        lineData.addDataSet(dataSetZ)

        onMeasurementUpdate(lineData, MeasType.Accelerometer)
    }

    private fun updateSpeedChart(speedData: ByteArray){
        var dataEntry = Entry(getMeasurementTime(), byteArrayToFloat(speedData))
        speedValsEntryList.add(dataEntry)
        val dataSet: LineDataSet = LineDataSet(speedValsEntryList, "Speed in kmh")
        dataSet.color = Color.RED
        dataSet.valueTextColor = Color.BLUE
        val lineData = LineData(dataSet)
        onMeasurementUpdate(lineData, MeasType.Speed)
    }

    private fun handleCharacteristicRead(characteristic: BluetoothGattCharacteristic, values: ByteArray){
        when (characteristic.uuid) {
            speedCharUuid -> updateSpeedChart(values)
            gyroXCharUuid -> updateGyroChart(values, Axis.X)
            gyroYCharUuid -> updateGyroChart(values, Axis.Y)
            gyroZCharUuid -> updateGyroChart(values, Axis.Z)
            accelXCharUuid -> updateAccelChart(values, Axis.X)
            accelYCharUuid -> updateAccelChart(values, Axis.Y)
            accelZCharUuid -> updateAccelChart(values, Axis.Z)
        }
    }

    private fun connectToDevice(){
        Log.d(TAG, "Scanning for BLE device")
        deviceManager.registerConnectionCallback(this::afterScanCallback)
        deviceManager!!.registerUpdaterCallback(this::handleCharacteristicRead)
        deviceManager.scanLeDevices(bluetoothLeScanner)
    }

    fun onConnectButtonClick(view: View){
        Log.i(TAG, "The connect button was clicked. Connection attempt running.")
        val snackbarConnect = Snackbar.make(view, R.string.connect_button_snackbar, Snackbar.LENGTH_LONG)
        snackbarConnect.show()
        initializeBluetoothObjects()
        Log.i(TAG, "BT objects were initialized")

        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth not available")
            // Device doesn't support Bluetooth
            val snackbarBtUnavailable = Snackbar.make(view, R.string.connect_button_unavailability, Snackbar.LENGTH_LONG)
            snackbarBtUnavailable.show()
            return
        }
        connectToDevice()
    }
}