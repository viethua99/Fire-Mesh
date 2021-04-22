package com.ceslab.firemesh.presentation.group_list.dialog.edit_group

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.util.AppUtil
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import com.siliconlab.bluetoothmesh.adk.data_model.group.GroupChangeNameException
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetChangeNameException
import timber.log.Timber
import javax.inject.Inject

class EditGroupViewModel @Inject constructor(
    private val meshNetworkManager: MeshNetworkManager

) : ViewModel() {
    private val isRemoveGroupSucceed = MutableLiveData<Boolean>()


    fun getRemoveGroupStatus() : LiveData<Boolean> {
        return isRemoveGroupSucceed
    }


    fun updateGroup(group: Group, newName: String) {
        if (!AppUtil.isNameValid(newName)) {
            return
        }

        try {
            group.name = newName
        } catch (e: GroupChangeNameException) {
            Timber.e("updateGroup exception: $e")
        }
    }

    fun removeGroup(groupToRemove: Group){
        Timber.d("groupToRemove")
        meshNetworkManager.removeGroup(groupToRemove,removeGroupCallback)
    }

    fun removeGroupLocally(group: Group) {
        group.removeOnlyFromLocalStructure()
    }

    private val removeGroupCallback  = object : MeshNetworkManager.RemoveGroupCallback {
        override fun success() {
            Timber.d("removeGroupCallback: success")
            isRemoveGroupSucceed.value = true
        }

        override fun error(group: Group?, errorType: ErrorType?) {
            Timber.e("removeGroupCallback: error: ${group!!.name} --- $errorType")
            isRemoveGroupSucceed.value = false
        }
    }
}