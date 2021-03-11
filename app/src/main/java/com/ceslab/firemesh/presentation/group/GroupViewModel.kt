package com.ceslab.firemesh.presentation.group

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
import com.siliconlab.bluetoothmesh.adk.ErrorType
import timber.log.Timber
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

    fun setListeners(){
        Timber.d("setListeners")
        meshConnectionManager.apply {
            addMeshConnectionListener(connectionStatusListener)
            addMeshMessageListener(connectionMessageListener)
        }
    }

    fun removeListeners(){
        Timber.d("removeListeners")
        meshConnectionManager.apply {
            removeMeshConnectionListener(connectionStatusListener)
            removeMeshMessageListener(connectionMessageListener)
        }
    }

    fun getMeshNodeList(): LiveData<Set<MeshNode>> {
        meshNodeList.value = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentGroup!!)
        return meshNodeList
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
}