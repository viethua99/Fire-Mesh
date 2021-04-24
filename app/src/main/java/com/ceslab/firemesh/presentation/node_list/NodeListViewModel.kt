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
import com.ceslab.firemesh.ota.utils.Converters.bytesToHexReversed
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
    fun startScan() {
        fireMeshScanner.startScanBle()
    }

    fun stopScan() {
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

    private fun getDataFlag(rawData: ByteArray): Byte {
        return rawData[0]
    }

    private fun getUserData(rawData: ByteArray): ByteArray {
        var userData = byteArrayOf()
        for (i in 1 until rawData.size) {
            userData += rawData[i]
        }
        return userData
    }

    private fun parsePeriodDataPacket(leftFlag:Int,dataArray: ByteArray): List<FireNodeStatus> {
        Timber.d("parsePeriodDataPacket: ${bytesToHex(dataArray)}")
        val nodeStatusList = mutableSetOf<FireNodeStatus>()
        val redundantValue = dataArray.size % 3
        for (i in 0 until (dataArray.size - redundantValue) step 3) {
            var gatewayType = MeshNode.GatewayType.NOT_GATEWAY
            if(i == 0){
                if(leftFlag == 0) { // Main gateway
                    gatewayType = MeshNode.GatewayType.MAIN_GATEWAY
                } else if(leftFlag == 1) { //Backup gateway
                    gatewayType = MeshNode.GatewayType.BACKUP_GATEWAY
                }
            }
            val unicastAddress = bytesToHex(byteArrayOf(dataArray[i + 1], dataArray[i]))
            val batteryPercent = byteToUnsignedInt(dataArray[i + 2])

            nodeStatusList.add(FireNodeStatus(batteryPercent, unicastAddress, gatewayType))
        }
        return nodeStatusList.toList()
    }

    private fun bindDataToMeshNode(dataFlag: Byte, userData: ByteArray) {
        val nodeList = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentSubnet!!)
        val leftFlag = ((dataFlag.toInt() and 0xF0) shr 4)
        val rightFlag = (dataFlag.toInt() and 0x0F)
        Timber.d("leftFlag = $leftFlag --rightFlag=$rightFlag")

        if (rightFlag == 0) { //Fire alarm signal
            val receivedUnicastAddress = bytesToHex(byteArrayOf(userData[1], userData[0]))
            for (node in nodeList) {
                val unicastAddress = "%4x".format(node.node.primaryElementAddress!!)
                if (unicastAddress == receivedUnicastAddress) {
                    node.fireSignal = 1
                }
            }
        } else if (rightFlag == 1) { //Heartbeat period signal
            val nodeStatusList = parsePeriodDataPacket(leftFlag,userData)
            for (node in nodeStatusList) {
                Timber.d(node.toString())
            }
            for (nodeStatus in nodeStatusList) {
                for (node in nodeList) {
                    val unicastAddress = "%4x".format(node.node.primaryElementAddress!!)
                    if (unicastAddress == nodeStatus.unicastAddress) {
                        node.gatewayType = nodeStatus.gatewayType
                        node.batteryPercent = nodeStatus.batteryPercent
                        if (nodeStatus.batteryPercent == 0xFF) { //Node is Death
                            node.heartBeat = 0
                        } else {
                            node.heartBeat = 1
                        }
                    }
                }
            }
        }

        getMeshNodeList()

    }

    private val fireMeshScanCallback = object : FireMeshScanner.FireMeshScannerCallback {
        override fun onScanResult(rawData: ByteArray) {
            Timber.d("onScanResult: ${Converters.bytesToHexWhitespaceDelimited(rawData)}")
            try {
                val dataFlag = getDataFlag(rawData)
                val encryptedUserData = getUserData(rawData)
                Timber.d(
                    "dataFlag = ${bytesToHex(byteArrayOf(dataFlag))} --encryptedUserData=${Converters.bytesToHexWhitespaceDelimited(
                        encryptedUserData
                    )} --size={${encryptedUserData.size}}"
                )

                val decryptedData = AESUtils.decrypt(
                    AESUtils.ECB_ZERO_BYTE_NO_PADDING_ALGORITHM,
                    AES_KEY,
                    encryptedUserData
                )
                Timber.d("decryptedData=  ${Converters.bytesToHexWhitespaceDelimited(decryptedData)} --size={${decryptedData.size}}")
                bindDataToMeshNode(dataFlag, decryptedData)

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