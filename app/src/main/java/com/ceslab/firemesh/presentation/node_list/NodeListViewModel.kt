package com.ceslab.firemesh.presentation.node_list

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConnectionManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNodeManager
import com.ceslab.firemesh.meshmodule.listener.ConnectionStatusListener
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.ceslab.firemesh.ota.utils.Converters
import timber.log.Timber
import javax.inject.Inject

class NodeListViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshNodeManager: MeshNodeManager,
    private val meshConnectionManager: MeshConnectionManager
) : ViewModel() {
    companion object {
        const val COMPANY_ID = 0x6969
    }
    private val meshNodeList = MutableLiveData<Set<MeshNode>>()

    fun setListeners() {
        Timber.d("setListeners")
        meshConnectionManager.addMeshConnectionListener(connectionStatusListener)
    }

    fun removeListener() {
        meshConnectionManager.removeMeshConnectionListener(connectionStatusListener)
    }

    fun setDeviceToConfigure(meshNode: MeshNode) {
        Timber.d("setDeviceToConfigure: ${meshNode.node.name}")
        bluetoothMeshManager.meshNodeToConfigure = meshNode
    }

    fun getMeshNodeList(): LiveData<Set<MeshNode>> {
        meshNodeList.value = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentSubnet!!)
        return meshNodeList
    }

    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            return bluetoothAdapter.bluetoothLeScanner
        }

    fun refreshNodeListStatus() {
        Timber.d("refreshNodeListStatus")
        val nodeList = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentSubnet!!)
        for (node in nodeList) {
            node.refresh()
        }
        getMeshNodeList()
    }

    fun scanNodeStatus() {
        Timber.d("scanNodeStatus")
        bluetoothLeScanner.startScan(scanCallback)
    }

    fun stopScan() {
        Timber.d("stopScan")
        bluetoothLeScanner.stopScan(scanCallback)
    }

    private fun checkFireAlarmSignalFromUnicastAddress(unicastAddress: ByteArray) {
        val hexUnicastAddress = Converters.bytesToHex(unicastAddress)
        Timber.d("checkFireAlarmSignalFromUnicastAddress: $hexUnicastAddress")
        val nodeList = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentSubnet!!)
        for (node in nodeList) {
            if (Integer.toHexString(node.node.primaryElementAddress!!) == hexUnicastAddress) {
                node.fireSignal = 1
            }
        }
        getMeshNodeList()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val rawData = result?.scanRecord?.bytes
            if (rawData != null) {
                Timber.d(Converters.bytesToHexWhitespaceDelimited(rawData))
            }

            val dataList = result?.scanRecord?.getManufacturerSpecificData(COMPANY_ID)
            if (dataList != null) {
                Timber.d("onScanResult: ${Converters.bytesToHex(dataList)}")
                checkFireAlarmSignalFromUnicastAddress(dataList)
            }
        }
    }

    private val connectionStatusListener = object : ConnectionStatusListener {
        override fun connecting() {}
        override fun disconnected() {}
        override fun connected() {
            Timber.d("connected")
            getMeshNodeList()
            scanNodeStatus()
        }


    }
}