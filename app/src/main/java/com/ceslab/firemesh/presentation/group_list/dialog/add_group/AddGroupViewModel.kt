package com.ceslab.firemesh.presentation.group_list.dialog.add_group

import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.util.AppUtil
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetCreationException
import timber.log.Timber
import javax.inject.Inject

class AddGroupViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshNetworkManager: MeshNetworkManager

) : ViewModel() {

    fun addGroup(newGroupName: String){
        Timber.d("addGroup: $newGroupName")
        if (!AppUtil.isNameValid(newGroupName)) {
            return
        }
        try {
            meshNetworkManager.createGroup(newGroupName,bluetoothMeshManager.currentSubnet!!)
        } catch (e: SubnetCreationException) {
            Timber.e("addGroup exception: $e")
        }

    }
}