package com.ceslab.firemesh.presentation.node_list

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConnectionManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNodeManager
import com.ceslab.firemesh.meshmodule.listener.ConnectionStatusListener
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.ota.utils.Converters
import com.ceslab.firemesh.service.FireMeshScanner
import timber.log.Timber
import javax.inject.Inject

class NodeListViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshNodeManager: MeshNodeManager,
    private val meshConnectionManager: MeshConnectionManager
) : ViewModel() {
    private val meshNodeList = MutableLiveData<Set<MeshNode>>()
    private val fireMeshScanner = FireMeshScanner.instance

    fun setListeners() {
        Timber.d("setListeners")
        meshConnectionManager.addMeshConnectionListener(connectionStatusListener)
        fireMeshScanner.addFireMeshScannerCallback(fireMeshScanCallback)
    }

    fun removeListener() {
        meshConnectionManager.removeMeshConnectionListener(connectionStatusListener)
        fireMeshScanner.removeFireMeshScannerCallback(fireMeshScanCallback)
    }

    fun setDeviceToConfigure(meshNode: MeshNode) {
        Timber.d("setDeviceToConfigure: ${meshNode.node.name}")
        bluetoothMeshManager.meshNodeToConfigure = meshNode
    }

    fun getMeshNodeList(): LiveData<Set<MeshNode>> {
        meshNodeList.value = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentSubnet!!)
        return meshNodeList
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startScan(){
        fireMeshScanner.startScanBle()
    }

    fun stopScan(){
        fireMeshScanner.stopScanBle()
    }


    fun refreshNodeListStatus() {
        Timber.d("refreshNodeListStatus")
        val nodeList = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentSubnet!!)
        for (node in nodeList) {
            node.refresh()
        }
        getMeshNodeList()
    }

    private fun checkFireAlarmSignalFromUnicastAddress(unicastAddress: ByteArray) {
        Timber.d("unicastAddress size = ${unicastAddress.size}")
        val hexUnicastAddress = Converters.bytesToHexReversed(unicastAddress)
        Timber.d("checkFireAlarmSignalFromUnicastAddress: $hexUnicastAddress")
        val nodeList = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentSubnet!!)
        for (node in nodeList ) {
            if (Integer.toHexString(node.node.primaryElementAddress!!) == hexUnicastAddress) {
                node.fireSignal = 1
            }
        }
        getMeshNodeList()
    }

    private val fireMeshScanCallback = object : FireMeshScanner.FireMeshScannerCallback {
        override fun onScanResult(dataList: ByteArray) {
            Timber.d("onScanResult: ${Converters.bytesToHexReversed(dataList)}")
            checkFireAlarmSignalFromUnicastAddress(dataList)
        }
    }

    private val connectionStatusListener = object : ConnectionStatusListener {
        override fun connecting() {}
        override fun disconnected() {}
        override fun connected() {
            Timber.d("connected")
            getMeshNodeList()
        }


    }
}