package com.ceslab.firemesh.presentation.group_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import javax.inject.Inject

class GroupListViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshNetworkManager: MeshNetworkManager
) : ViewModel() {
    private val groupList = MutableLiveData<Set<Group>>()

    init {
        groupList.value = bluetoothMeshManager.currentSubnet!!.groups
    }

    fun getGroupList() : LiveData<Set<Group>> {
        return groupList
    }
}