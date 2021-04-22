package com.ceslab.firemesh.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.Build
import androidx.annotation.RequiresApi
import com.ceslab.firemesh.myapp.COMPANY_ID
import com.ceslab.firemesh.ota.utils.Converters
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

class FireMeshScanner private constructor() {
    private val fireMeshScannerCallbackList: ArrayList<FireMeshScannerCallback> = ArrayList()

    companion object {
        val instance = FireMeshScanner()
    }

    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            return bluetoothAdapter.bluetoothLeScanner
        }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startScanBle() {
        Timber.d("startScanBle")
        val filterBuilder = ScanFilter.Builder()
        val filter = filterBuilder.setManufacturerData(COMPANY_ID, byteArrayOf()).build()
        val settingBuilder = ScanSettings.Builder()
        settingBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        val setting = settingBuilder
            .setLegacy(false)
            .build()
        bluetoothLeScanner.startScan(listOf(filter), setting, scanCallback)
    }

    fun stopScanBle() {
        Timber.d("stopScanBle")
        bluetoothLeScanner.stopScan(scanCallback)
    }

    fun addFireMeshScannerCallback(scannerCallback: FireMeshScannerCallback) {
        synchronized(fireMeshScannerCallbackList) {
            fireMeshScannerCallbackList.add(scannerCallback)
        }
    }

    fun removeFireMeshScannerCallback(scannerCallback: FireMeshScannerCallback) {
        synchronized(fireMeshScannerCallbackList) {
            fireMeshScannerCallbackList.remove(scannerCallback)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val rawData = result?.scanRecord?.bytes
            if (rawData != null) {
                Timber.d("Raw: " + Converters.bytesToHexWhitespaceDelimited(rawData))
                val encryptedData = result.scanRecord?.getManufacturerSpecificData(COMPANY_ID)
                Timber.d("Encrypted data: " + Converters.bytesToHexWhitespaceDelimited(encryptedData))
                fireMeshScannerCallbackList.forEach { listener -> listener.onScanResult(encryptedData!!) }

            }


        }
    }

    interface FireMeshScannerCallback {
        fun onScanResult(dataList: ByteArray)
    }


    //************TIMER TASK JUST FOR TEST BACKGROUND SERVICE********//
    private val timerCallbackList: ArrayList<TimerCallback> = ArrayList()
    fun addTimerCallback(timerCallback: TimerCallback) {
        synchronized(timerCallbackList) {
            timerCallbackList.add(timerCallback)
        }
    }

    fun removeTimerCallback(timerCallback: TimerCallback) {
        synchronized(timerCallbackList) {
            timerCallbackList.remove(timerCallback)
        }
    }

    private var counter = 0
    private var timer: Timer? = null
    private lateinit var timerTask: TimerTask

    interface TimerCallback {
        fun onScanResult(counter: Int)
    }

    fun startTimer() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                Timber.i("Count: --- ${counter++}")
                timerCallbackList.forEach { listener -> listener.onScanResult(counter) }

            }
        }
        timer!!.schedule(timerTask, 1000, 1000)
    }

    fun stopTimerTask() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }
    //*****************************************************************//

}