package com.ceslab.firemesh.presentation.node_list

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
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
import com.ceslab.firemesh.myapp.COMPANY_ID
import com.ceslab.firemesh.ota.utils.Converters
import timber.log.Timber
import javax.inject.Inject

class NodeListViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshNodeManager: MeshNodeManager,
    private val meshConnectionManager: MeshConnectionManager
) : ViewModel() {
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


    fun refreshNodeListStatus() {
        Timber.d("refreshNodeListStatus")
        val nodeList = meshNodeManager.getMeshNodeList(bluetoothMeshManager.currentSubnet!!)
        for (node in nodeList) {
            node.refresh()
        }
        getMeshNodeList()
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