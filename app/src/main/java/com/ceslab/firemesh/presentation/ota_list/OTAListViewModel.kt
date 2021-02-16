package com.ceslab.firemesh.presentation.ota_list

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.ota.OTAConfigListener
import com.ceslab.firemesh.meshmodule.ota.OTADevice
import com.ceslab.firemesh.meshmodule.ota.OTAManager
import timber.log.Timber
import javax.inject.Inject

class OTAListViewModel @Inject constructor(
    private val context: Context,
    private val otaManager: OTAManager
) : ViewModel(){
    val scannedDeviceResult = MutableLiveData<OTADevice>()
    private val scanStatus = MutableLiveData<Boolean>()
    private val deviceConnectStatus = MutableLiveData<String>()


    fun scanOTADevice() {
        Timber.d("scanOTADevice")
        var isLeScanStarted = otaManager.isLeScanStarted()
        if (isLeScanStarted) {
            otaManager.stopLeScan()
            isLeScanStarted = false
        } else {
            otaManager.startLeScan(null)
            isLeScanStarted = true
        }
        scanStatus.value = isLeScanStarted
    }

    fun connectDevice(otaDevice: OTADevice){
        Timber.d("connectDevice: $otaDevice")
        otaManager.connectDevice(context,otaDevice.deviceAddress)
    }

    fun isLeScanStarted(): LiveData<Boolean> {
        return scanStatus
    }

    fun getConnectStatus():LiveData<String> {
        return deviceConnectStatus
    }

    fun stopScan(){
        Timber.d("stopScan")
        if(otaManager.isLeScanStarted()){
            otaManager.stopLeScan()
            scanStatus.value = false
        }
    }

    private fun addDeviceToOTAList(device: OTADevice) {
        Timber.d("addDevice=${device.deviceName}")
        scannedDeviceResult.value = device
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Timber.d("onScanResult: ${result?.scanRecord!!.serviceUuids}")
            if (result.scanRecord == null ||
                result.scanRecord!!.serviceUuids == null ||
                result.scanRecord!!.serviceUuids.isEmpty()
            ) {
                return
            }
                addDeviceToOTAList(OTADevice(result.device.name,result.device.address))
        }
    }

    private val otaConfigListener = object  : OTAConfigListener {
        override fun onGattConnected() {
           Timber.d("onGattConnected")
            deviceConnectStatus.value = "GATT_CONNECTED"
        }

        override fun onGattDisconnected() {
            Timber.d("onGattDisconnected")
            deviceConnectStatus.value = "GATT_DISCONNECTED"
        }

        override fun onGattConnectFailed(status: Int) {
            Timber.e("onGattConnectFailed: $status")
            deviceConnectStatus.value = "GATT_FAILED"
        }
    }

    init {
        otaManager.addScanCallback(scanCallback)
        otaManager.addOtaConfigListener(otaConfigListener)
    }
}