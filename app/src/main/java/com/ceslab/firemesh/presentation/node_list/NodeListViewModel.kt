package com.ceslab.firemesh.presentation.node_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNodeManager
import com.ceslab.firemesh.meshmodule.model.MeshNode
import timber.log.Timber
import javax.inject.Inject

class NodeListViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshNodeManager: MeshNodeManager
) : ViewModel(){
    private val meshNodeList = MutableLiveData<Set<MeshNode>>()

    fun setDeviceToConfigure(meshNode: MeshNode){
        Timber.d("setDeviceToConfigure: ${meshNode.node.name}")
        bluetoothMeshManager.meshNodeToConfigure = meshNode
    }

    fun getMeshNodeList(): LiveData<Set<MeshNode>> {
        meshNodeList.value = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentSubnet!!)
        return meshNodeList
    }
}