package com.ceslab.firemesh.presentation.node.node_info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConnectionManager
import com.ceslab.firemesh.meshmodule.listener.MeshLoadedListener
import com.ceslab.firemesh.meshmodule.model.MeshNode
import timber.log.Timber
import javax.inject.Inject

class NodeInfoViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshConnectionManager: MeshConnectionManager
) : ViewModel() {

   private val meshNodeToConfigure = MutableLiveData<MeshNode>()

    fun getMeshNodeToConfigure(): LiveData<MeshNode> {
        meshConnectionManager.addMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
        return meshNodeToConfigure
    }

    fun removeMeshConfigurationLoadedListener() {
        Timber.d("removeMeshConfigurationLoadedListener")
        meshConnectionManager.removeMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
    }

    private val meshConfigurationLoadedListener = object : MeshLoadedListener {
        override fun initialConfigurationLoaded() {
            Timber.d("initialConfigurationLoaded")
            meshNodeToConfigure.value = bluetoothMeshManager.meshNodeToConfigure!!
        }
    }
}