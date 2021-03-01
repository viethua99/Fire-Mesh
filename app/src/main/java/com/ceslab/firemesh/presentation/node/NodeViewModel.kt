package com.ceslab.firemesh.presentation.node

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConnectionManager
import com.ceslab.firemesh.meshmodule.listener.MeshLoadedListener
import com.ceslab.firemesh.meshmodule.listener.ConnectionStatusListener
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import timber.log.Timber
import javax.inject.Inject

class NodeViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshConnectionManager: MeshConnectionManager
) : ViewModel() {
    var isFirstConfig = false
    private val meshStatus = MutableLiveData<MeshStatus>()
    private val meshNodeToConfigure = bluetoothMeshManager.meshNodeToConfigure!!

    fun connectToNode() {
        Timber.d("connectToNode")
        meshConnectionManager.apply {
            addMeshConnectionListener(meshConnectionListener)
            addMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
        }

        if(isFirstConfig){
            meshConnectionManager.connect(bluetoothMeshManager.provisionedMeshConnectableDevice!!, true)
        } else {

        }
    }

    fun disconnectFromNode(){
        Timber.d("disconnectFromNode")
        meshConnectionManager.apply {
            removeMeshConnectionListener(meshConnectionListener)
            removeMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
        }

        if(isFirstConfig){
            meshConnectionManager.disconnect()
        }
    }

    fun getMeshStatus(): LiveData<MeshStatus> {
        return meshStatus
    }

    private val meshConnectionListener = object : ConnectionStatusListener {
        override fun connecting() {
            Timber.d("connecting")
            meshStatus.value = MeshStatus.MESH_CONNECTING
        }

        override fun connected() {
            Timber.d("connected")
            if(isFirstConfig){
                meshConnectionManager.setupInitialNodeConfiguration(meshNodeToConfigure.node)
            }
            meshStatus.value = MeshStatus.MESH_CONNECTED

        }

        override fun disconnected() {
            Timber.d("disconnected")
            meshStatus.value = MeshStatus.MESH_DISCONNECTED
        }
    }

    private val meshConfigurationLoadedListener = object : MeshLoadedListener {
        override fun initialConfigurationLoaded() {
            Timber.d("initialConfigurationLoaded")
            meshStatus.value = MeshStatus.INIT_CONFIGURATION_LOADED
        }
    }

}