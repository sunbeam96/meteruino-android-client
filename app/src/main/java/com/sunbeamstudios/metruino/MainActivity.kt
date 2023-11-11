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
import java.nio.charset.Charset
import java.util.Arrays

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val deviceManager = DeviceManager()
    //
    var speedValsEntryList = ArrayList<Entry>()
    var gyroXValsEntryList = ArrayList<Entry>()
    var gyroYValsEntryList = ArrayList<Entry>()
    var gyroZValsEntryList = ArrayList<Entry>()
    var accelXValsEntryList = ArrayList<Entry>()
    var accelYValsEntryList = ArrayList<Entry>()
    var accelZValsEntryList = ArrayList<Entry>()
    private var startTime: Long = System.currentTimeMillis()
    private var isCollectionFreezed = false

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

    private fun updateGyroChart(gyroData: List<Char>){
        val separator = ""
        val gyroX = gyroData.slice(0..3).joinToString(separator).replace(" ", "").toFloat()
        val gyroY = gyroData.slice(4..7).joinToString(separator).replace(" ", "").toFloat()
        val gyroZ = gyroData.slice(8..11).joinToString(separator).replace(" ", "").toFloat()
        var xDataEntry = Entry(getMeasurementTime(), gyroX)
        var yDataEntry = Entry(getMeasurementTime(), gyroY)
        var zDataEntry = Entry(getMeasurementTime(), gyroZ)
        val lineData = LineData()
        gyroXValsEntryList.add(xDataEntry)
        gyroYValsEntryList.add(yDataEntry)
        gyroZValsEntryList.add(zDataEntry)

        val dataSetX = LineDataSet(gyroXValsEntryList, "Gyro X")
        dataSetX.color = Color.RED
        dataSetX.valueTextColor = Color.BLUE
        lineData.addDataSet(dataSetX)

        val dataSetY = LineDataSet(gyroYValsEntryList, "Gyro Y")
        dataSetY.color = Color.BLACK
        dataSetY.valueTextColor = Color.YELLOW
        lineData.addDataSet(dataSetY)

        val dataSetZ = LineDataSet(gyroZValsEntryList, "Gyro Z")
        dataSetZ.color = Color.CYAN
        dataSetZ.valueTextColor = Color.GRAY
        lineData.addDataSet(dataSetZ)

        onMeasurementUpdate(lineData, MeasType.Gyroscope)
    }

    private fun updateAccelChart(accelData: List<Char>){
        val separator = ""
        val accelX = accelData.slice(0..3).joinToString(separator).replace(" ", "").toFloat()
        val accelY = accelData.slice(4..7).joinToString(separator).replace(" ", "").toFloat()
        val accelZ = accelData.slice(8..11).joinToString(separator).replace(" ", "").toFloat()
        var xDataEntry = Entry(getMeasurementTime(), accelX)
        var yDataEntry = Entry(getMeasurementTime(), accelY)
        var zDataEntry = Entry(getMeasurementTime(), accelZ)
        val lineData = LineData()
        accelXValsEntryList.add(xDataEntry)
        accelYValsEntryList.add(yDataEntry)
        accelZValsEntryList.add(zDataEntry)

        val dataSetX: LineDataSet = LineDataSet(accelXValsEntryList, "Accel X")
        dataSetX.color = Color.RED
        dataSetX.valueTextColor = Color.BLUE
        dataSetX.circleColors = listOf(Color.RED)
        lineData.addDataSet(dataSetX)

        val dataSetY: LineDataSet = LineDataSet(accelYValsEntryList, "Accel Y")
        dataSetY.color = Color.BLACK
        dataSetY.valueTextColor = Color.YELLOW
        dataSetY. = listOf(Color.BLACK)
        lineData.addDataSet(dataSetY)

        val dataSetZ: LineDataSet = LineDataSet(accelZValsEntryList, "Accel Z")
        dataSetZ.color = Color.CYAN
        dataSetZ.valueTextColor = Color.GRAY
        dataSetZ.circleColors = listOf(Color.CYAN)
        lineData.addDataSet(dataSetZ)

        onMeasurementUpdate(lineData, MeasType.Accelerometer)
    }

    private fun updateSpeedChart(speedData: Float){
        var dataEntry = Entry(getMeasurementTime(), speedData)
        speedValsEntryList.add(dataEntry)
        val dataSet: LineDataSet = LineDataSet(speedValsEntryList, "Speed in kmh")
        dataSet.color = Color.RED
        dataSet.valueTextColor = Color.BLUE
        val lineData = LineData(dataSet)
        onMeasurementUpdate(lineData, MeasType.Speed)
    }

    private fun byteArrayToCharArray(collectiveData: ByteArray) : CharArray{
        val charset = Charset.forName("UTF-8")
        val charBuffer = charset.decode(ByteBuffer.wrap(collectiveData))
        return Arrays.copyOf(charBuffer.array(), charBuffer.limit())
    }

    private fun updateChartViaCollectiveCharacteristic(collectiveData: ByteArray){
        val charArray = byteArrayToCharArray(collectiveData)
        val separator = ""

        //SPEED
        val speedArray = charArray.slice(0..2)
        val speed = speedArray.joinToString(separator).replace(" ", "").toFloat()
        updateSpeedChart(speed)

        //ACCELEROMETER
        val accelArray = charArray.slice(3..14)
        updateAccelChart(accelArray)

        //GYROSCOPE
        val gyroArray = charArray.slice(15..26)
        updateGyroChart(gyroArray)
    }

    private fun handleCharacteristicRead(characteristic: BluetoothGattCharacteristic, values: ByteArray){
        if (!isCollectionFreezed)
            updateChartViaCollectiveCharacteristic(values)
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

    fun onMeasurementStopTriggerButtonClick(view: View){
        isCollectionFreezed = !isCollectionFreezed
    }

    fun onRefreshButtonClick(view: View){
        Log.i(TAG, "Refresh button clicked.")
        speedValsEntryList.clear()
        gyroXValsEntryList.clear()
        gyroYValsEntryList.clear()
        gyroZValsEntryList.clear()
        accelXValsEntryList.clear()
        accelYValsEntryList.clear()
        accelZValsEntryList.clear()
        startTime = System.currentTimeMillis()
    }
}
