package com.ceslab.firemesh.presentation.group_list.dialog.add_group

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.util.AppUtil
import com.siliconlab.bluetoothmesh.adk.data_model.network.SubnetMaxExceededException
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.GroupCreationException
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.GroupMaxExceededException
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetCreationException
import timber.log.Timber
import javax.inject.Inject

class AddGroupViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshNetworkManager: MeshNetworkManager

) : ViewModel() {
    val errorMessage = MutableLiveData<String>()


    fun addGroup(newGroupName: String){
        Timber.d("addGroup: $newGroupName")
        if (!AppUtil.isNameValid(newGroupName)) {
            errorMessage.value = "Group name is not valid"

            return
        }
        try {
            meshNetworkManager.createGroup(newGroupName,bluetoothMeshManager.currentSubnet!!)
        } catch (e: GroupCreationException) {
            Timber.e("addGroup exception: $e")
            if(e.cause is GroupMaxExceededException){
                errorMessage.value = "Max number of group reached"
            }
        }

    }
}