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

    private val meshStatus = MutableLiveData<MeshStatus>()
    private val meshNodeToConfigure = bluetoothMeshManager.meshNodeToConfigure!!

    fun connectToNode() {
        Timber.d("connectToNode")
        meshConnectionManager.addMeshConnectionListener(meshConnectionListener)
        meshConnectionManager.addMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
        meshConnectionManager.connect(bluetoothMeshManager.provisionedMeshConnectableDevice!!, true)
    }

    fun disconnectFromNode(){
        Timber.d("disconnectFromNode")
        meshConnectionManager.removeMeshConnectionListener(meshConnectionListener)
        meshConnectionManager.removeMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
        meshConnectionManager.disconnect()
    }

    fun getMeshStatus(): LiveData<MeshStatus> {
        return meshStatus
    }

    private val meshConnectionListener = object : ConnectionStatusListener {
        override fun connecting() {
            Timber.d("connecting")
            meshStatus.value = MeshStatus.CONNECTING_NODE
        }

        override fun connected() {
            Timber.d("connected")
            meshConnectionManager.setupInitialNodeConfiguration(meshNodeToConfigure.node)
            meshStatus.value = MeshStatus.CONNECTED_NODE

        }

        override fun disconnected() {
            Timber.d("disconnected")
            meshStatus.value = MeshStatus.DISCONNECTED_NODE
        }
    }

    private val meshConfigurationLoadedListener = object : MeshLoadedListener {
        override fun initialConfigurationLoaded() {
            Timber.d("initialConfigurationLoaded")
            meshStatus.value = MeshStatus.INIT_CONFIGURATION_LOADED
        }
    }

}