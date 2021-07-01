package com.ceslab.firemesh.presentation.group

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConnectionManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNodeManager
import com.ceslab.firemesh.meshmodule.listener.ConnectionMessageListener
import com.ceslab.firemesh.meshmodule.listener.ConnectionStatusListener
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.ceslab.firemesh.myapp.AES_KEY
import com.ceslab.firemesh.ota.utils.Converters
import com.ceslab.firemesh.service.FireMeshScanner
import com.ceslab.firemesh.service.FireNodeStatus
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlabs.bluetoothmesh.App.AESUtils
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

/**
 * Created by Viet Hua on 03/11/2021.
 */

class GroupViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshConnectionManager: MeshConnectionManager,
    private val meshNodeManager: MeshNodeManager

    ) : ViewModel() {

    private val meshStatus = MutableLiveData<MeshStatus>()
    private val connectionMessage = MutableLiveData<ConnectionMessageListener.MessageType>()
    private val errorMessage = MutableLiveData<ErrorType>()
    private val currentSubnet = bluetoothMeshManager.currentSubnet!!
    private val meshNodeList = MutableLiveData<Set<MeshNode>>()
    private val fireMeshScanner = FireMeshScanner.instance

    fun setListeners(){
        Timber.d("setListeners")
        meshConnectionManager.apply {
            addMeshConnectionListener(connectionStatusListener)
            addMeshMessageListener(connectionMessageListener)
            fireMeshScanner.addFireMeshScannerCallback(fireMeshScanCallback)

        }
    }

    fun removeListeners(){
        Timber.d("removeListeners")
        meshConnectionManager.apply {
            removeMeshConnectionListener(connectionStatusListener)
            removeMeshMessageListener(connectionMessageListener)
            fireMeshScanner.removeFireMeshScannerCallback(fireMeshScanCallback)

        }
    }


    fun getNodeListSize(): Int{
        return currentSubnet.nodes.size
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun startScan() {
        fireMeshScanner.startScanBle()
    }

    fun stopScan() {
        fireMeshScanner.stopScanBle()
    }

    fun getMeshNodeList(): LiveData<Set<MeshNode>> {
        meshNodeList.value = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentGroup!!)
        return meshNodeList
    }

    fun refreshNodeListStatus() {
        Timber.d("refreshNodeListStatus")
        val nodeList = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentGroup!!)
        for (node in nodeList) {
            node.refresh()
        }
        getMeshNodeList()
    }

    fun changeMeshStatus(meshStatus: MeshStatus){
        Timber.d("changeMeshStatus: $meshStatus")
        when (meshStatus) {
            MeshStatus.MESH_CONNECTING -> {
                meshConnectionManager.disconnect()
            }
            MeshStatus.MESH_CONNECTED -> {
                meshConnectionManager.disconnect()
            }
            MeshStatus.MESH_DISCONNECTED -> {
                meshConnectionManager.connect(currentSubnet)
            }
        }
    }

    fun getMeshStatus(): LiveData<MeshStatus> {
        return meshStatus
    }

    fun getConnectionMessage(): LiveData<ConnectionMessageListener.MessageType> {
        return connectionMessage
    }

    fun getErrorMessage(): LiveData<ErrorType> {
        return errorMessage
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
        Timber.d("parsePeriodDataPacket: ${Converters.bytesToHex(dataArray)}")
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
            val unicastAddress = Converters.bytesToHex(byteArrayOf(dataArray[i + 1], dataArray[i]))
            val batteryPercent = Converters.byteToUnsignedInt(dataArray[i + 2])

            nodeStatusList.add(FireNodeStatus(batteryPercent, unicastAddress, gatewayType))
        }

        return nodeStatusList.filter { it.unicastAddress != "0000" }.toList()
    }

    private fun bindDataToMeshNode(dataFlag: Byte, userData: ByteArray) {
        val nodeList = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentGroup!!)
        val leftFlag = ((dataFlag.toInt() and 0xF0) shr 4)
        val rightFlag = (dataFlag.toInt() and 0x0F)
        Timber.d("leftFlag = $leftFlag --rightFlag=$rightFlag")

        if (rightFlag == 0) { //Fire alarm signal
            val receivedUnicastAddress =
                Converters.bytesToHex(byteArrayOf(userData[1], userData[0]))
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

    private val connectionStatusListener = object : ConnectionStatusListener {
        override fun connecting() {
            Timber.d("connecting")
            meshStatus.value = MeshStatus.MESH_CONNECTING
        }

        override fun connected() {
            Timber.d("connected")
            meshStatus.value = MeshStatus.MESH_CONNECTED

        }

        override fun disconnected() {
            Timber.d("disconnected")
            meshStatus.value = MeshStatus.MESH_DISCONNECTED
        }
    }

    private val connectionMessageListener = object : ConnectionMessageListener {
        override fun connectionMessage(messageType: ConnectionMessageListener.MessageType) {
            Timber.d("connectionMessage: $messageType")
            connectionMessage.value = messageType
        }

        override fun connectionErrorMessage(error: ErrorType) {
            Timber.e("connectionErrorMessage: $error")
            errorMessage.value = error
        }
    }

    private val fireMeshScanCallback = object : FireMeshScanner.FireMeshScannerCallback {
        override fun onScanResult(rawData: ByteArray?) {
            rawData?.let {
                Timber.d("onScanResult: ${Converters.bytesToHexWhitespaceDelimited(it)}")
                try {
                    val dataFlag = getDataFlag(it)
                    val encryptedUserData = getUserData(it)
                    Timber.d(
                        "dataFlag = ${Converters.bytesToHex(byteArrayOf(dataFlag))} --encryptedUserData=${Converters.bytesToHexWhitespaceDelimited(
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
    }
}