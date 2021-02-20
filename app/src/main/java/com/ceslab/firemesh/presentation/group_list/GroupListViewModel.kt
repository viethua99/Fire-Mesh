package com.ceslab.firemesh.presentation.group_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import timber.log.Timber
import javax.inject.Inject

class GroupListViewModel @Inject constructor(
    private val meshNetworkManager: MeshNetworkManager,
    private val bluetoothMeshManager: BluetoothMeshManager) : ViewModel() {

    private val groupList = MutableLiveData<Set<Group>>()
    private val isRemoveGroupSucceed = MutableLiveData<Boolean>()


    fun getGroupList() : LiveData<Set<Group>> {
        groupList.value = bluetoothMeshManager.currentSubnet!!.groups
        return groupList
    }

    fun getRemoveGroupStatus() : LiveData<Boolean> {
        return isRemoveGroupSucceed
    }

    fun removeGroup(groupToRemove: Group){
        Timber.d("groupToRemove")
        meshNetworkManager.removeGroup(groupToRemove,removeGroupCallback)
    }

    private val removeGroupCallback  = object : MeshNetworkManager.RemoveGroupCallback {
        override fun success() {
            Timber.d("removeSubnetCallback: success")
            groupList.value = bluetoothMeshManager.currentSubnet!!.groups
            isRemoveGroupSucceed.value = true
        }

        override fun error(group: Group?, errorType: ErrorType?) {
            Timber.e("removeSubnetCallback: error: ${group!!.name} --- $errorType")
            isRemoveGroupSucceed.value = false
        }
    }


}