package com.ceslab.firemesh.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.Build
import androidx.annotation.RequiresApi
import com.ceslab.firemesh.myapp.COMPANY_ID
import com.ceslab.firemesh.ota.utils.Converters
import timber.log.Timber

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
        val filter = filterBuilder.build()
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

    fun addFireMeshScannerCallback(scannerCallback: FireMeshScannerCallback){
        synchronized(fireMeshScannerCallbackList){
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
            }

            val dataList = result?.scanRecord?.getManufacturerSpecificData(COMPANY_ID)
            if (dataList != null) {
                fireMeshScannerCallbackList.forEach { listener -> listener.onScanResult(dataList) }
            }
        }
    }

    interface FireMeshScannerCallback {
        fun onScanResult(dataList: ByteArray)
    }
}