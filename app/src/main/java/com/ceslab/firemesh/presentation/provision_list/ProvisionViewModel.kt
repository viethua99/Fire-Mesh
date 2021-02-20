package com.ceslab.firemesh.presentation.provision_list

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.ParcelUuid
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothle.BluetoothScanner
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.model.ConnectableDeviceDescription
import com.ceslab.firemesh.meshmodule.model.MeshConnectableDevice
import com.siliconlab.bluetoothmesh.adk.provisioning.ProvisionerConnection
import timber.log.Timber
import javax.inject.Inject

class ProvisionViewModel @Inject constructor(
    private val context: Context,
    private val bluetoothScanner: BluetoothScanner,
    private val bluetoothMeshManager: BluetoothMeshManager
) : ViewModel() {

    val scannedDeviceResult = MutableLiveData<ConnectableDeviceDescription>()
    private val scanStatus = MutableLiveData<Boolean>()

    fun scanUnprovisionedDevice() {
        Timber.d("scanUnprovisionedDevice")
        var isLeScanStarted = bluetoothScanner.isLeScanStarted()
        if (isLeScanStarted) {
            bluetoothScanner.stopLeScan()
            isLeScanStarted = false
        } else {
            val meshService =
                ParcelUuid.fromString(ProvisionerConnection.MESH_UNPROVISIONED_SERVICE.toString())
            bluetoothScanner.startLeScan(meshService)
            isLeScanStarted = true
        }
        scanStatus.value = isLeScanStarted
    }

    fun isLeScanStarted(): LiveData<Boolean> {
        return scanStatus
    }

    fun stopScan(){
        Timber.d("stopScan")
        if(bluetoothScanner.isLeScanStarted()){
            bluetoothScanner.stopLeScan()
            scanStatus.value = false
        }
    }

    private fun addDeviceToScannerList(connectableDeviceDescription: ConnectableDeviceDescription) {
        Timber.d("addDevice=${connectableDeviceDescription.deviceName}")
        scannedDeviceResult.value = connectableDeviceDescription
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Timber.d("onScanResult: ${result?.device?.address}")
            if (result?.scanRecord == null ||
                result.scanRecord!!.serviceUuids == null ||
                result.scanRecord!!.serviceUuids.isEmpty()
            ) {
                return
            }
            val bluetoothConnectableDevice =
                MeshConnectableDevice(context, result, bluetoothScanner)
            val unprovisionedDevices =
                bluetoothMeshManager.bluetoothMesh.connectableDeviceHelper.filterUnprovisionedDevices(
                    listOf(bluetoothConnectableDevice)
                )
            if (unprovisionedDevices.isNotEmpty()) {
                val connectableDeviceDescription =
                    ConnectableDeviceDescription.Builder.build(result, bluetoothConnectableDevice)
                addDeviceToScannerList(connectableDeviceDescription)
            }
        }
    }



    init {
        bluetoothScanner.addScanCallback(scanCallback)
    }
}