package com.ceslab.ble_mesh_core.bluetoothle


import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import java.util.ArrayList
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by Viet Hua on 11/23/2020.
 */

class BluetoothScanner(bluetoothStateReceiver: BluetoothStateReceiver) : ScanCallback(), BluetoothStateReceiver.BluetoothStateListener {
    private val TAG: String = javaClass.canonicalName!!

    private var leScanStarted: Boolean = false

    private var bluetoothLeScanner: BluetoothLeScanner? = null

    private val scanCallbacks = ConcurrentLinkedQueue<ScanCallback>()

    init {
        bluetoothStateReceiver.addListener(this)
    }

    fun addScanCallback(scanCallback: ScanCallback) {
        scanCallbacks.add(scanCallback)
    }

    fun removeScanCallback(scanCallback: ScanCallback) {
        scanCallbacks.remove(scanCallback)
    }

    fun startLeScan(meshServ: ParcelUuid? = null): Boolean {
        synchronized(leScanStarted) {
            if (leScanStarted) {
                return true
            }
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
                return false
            }

            initBluetoothLeScanner()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            val filters = ArrayList<ScanFilter>()
            meshServ?.let {
                val filter = ScanFilter.Builder()
                    .setServiceUuid(meshServ)
                    .build()
                filters.add(filter)
            }
            bluetoothLeScanner?.apply {
                startScan(filters, settings, this@BluetoothScanner)
                leScanStarted = true
                return true
            }
            return false
        }
    }

    fun stopLeScan() {
        synchronized(leScanStarted) {
            if (!leScanStarted) {
                return
            }
            bluetoothLeScanner?.apply {
                stopScan(this@BluetoothScanner)
                leScanStarted = false
            }
        }
    }

    fun isLeScanStarted(): Boolean {
        return leScanStarted
    }

    //

    private fun initBluetoothLeScanner() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    }

    // ScanCallback

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        super.onScanResult(callbackType, result)
        synchronized(leScanStarted) {
            if (!leScanStarted) {
                return
            }
        }
        scanCallbacks.forEach { callback ->
            callback.onScanResult(callbackType, result)
        }
    }

    // BluetoothStateListener

    override fun onBluetoothStateChanged(enabled: Boolean) {
        synchronized(leScanStarted) {
            Log.d(TAG, "onBluetoothStateChanged")
            if (leScanStarted) {
                leScanStarted = false
            }
        }
    }
}