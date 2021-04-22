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
import com.ceslab.firemesh.myapp.AES_KEY
import com.ceslab.firemesh.ota.utils.Converters
import com.ceslab.firemesh.ota.utils.Converters.byteToUnsignedInt
import com.ceslab.firemesh.ota.utils.Converters.bytesToHex
import com.ceslab.firemesh.service.FireMeshScanner
import com.ceslab.firemesh.service.FireNodeStatus
import com.siliconlabs.bluetoothmesh.App.AESUtils
import timber.log.Timber
import java.lang.Exception
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

    private fun parseDataPacket(dataArray:ByteArray): List<FireNodeStatus>{
        val nodeStatusList = ArrayList<FireNodeStatus>()

        for (i in dataArray.indices step 3) {
            val unicastAddress = bytesToHex(byteArrayOf(dataArray[i+1],dataArray[i]))
            val batteryPercent = byteToUnsignedInt(dataArray[i+2])
            Timber.d("parseDataPacket: unicastAddress=$unicastAddress-- battery=$batteryPercent")
            nodeStatusList.add(FireNodeStatus(batteryPercent,unicastAddress))
        }
       return nodeStatusList
    }

    private fun bindDataToMeshNode(dataList: ByteArray){
        Timber.d("bindDataToMeshNode:dataList={${Converters.bytesToHexWhitespaceDelimited(dataList)}} ---- size = ${dataList.size}")
        val nodeList = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentSubnet!!)
        if(dataList.size == 2){ //Fire alarm signal
            val receivedUnicastAddress = bytesToHex(dataList.reversedArray())
            for (node in nodeList ) {
                val unicastAddress = "%4x".format(node.node.primaryElementAddress!!)
                if (unicastAddress == receivedUnicastAddress) {
                    node.fireSignal = 1
                }
            }
        } else {
            val nodeStatusList = parseDataPacket(dataList) //Heartbeat period signal
            for(nodeStatus in nodeStatusList){
                for (node in nodeList ) {
                    val unicastAddress = "%4x".format(node.node.primaryElementAddress!!)
                    if (unicastAddress == nodeStatus.unicastAddress) {
                        node.batteryPercent = nodeStatus.batteryPercent
                    }
                }
            }
        }
        getMeshNodeList()

    }

    private val fireMeshScanCallback = object : FireMeshScanner.FireMeshScannerCallback {
        override fun onScanResult(dataList: ByteArray) {
            Timber.d("onScanResult: ${Converters.bytesToHexWhitespaceDelimited(dataList)}")
            try {
                val decryptedData = AESUtils.decrypt(AESUtils.ECB_ZERO_BYTE_PADDING_ALGORITHM, AES_KEY, dataList)
                Timber.d("decryptedData=  ${Converters.bytesToHexWhitespaceDelimited(decryptedData)} --size={${decryptedData.size}}")
                bindDataToMeshNode(decryptedData)

            } catch (exception: Exception) {
                exception.printStackTrace()
            }
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