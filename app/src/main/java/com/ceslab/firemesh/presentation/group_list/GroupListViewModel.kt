package com.ceslab.firemesh.presentation.group_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.util.AppUtil
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetCreationException
import timber.log.Timber
import javax.inject.Inject

class GroupListViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager) : ViewModel() {
    private val groupList = MutableLiveData<Set<Group>>()


    fun getGroupList() : LiveData<Set<Group>> {
        groupList.value = bluetoothMeshManager.currentSubnet!!.groups
        return groupList
    }


}