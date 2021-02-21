package com.ceslab.firemesh.presentation.subnet_list.dialog.edit_subnet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.util.AppUtil
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetChangeNameException
import timber.log.Timber
import javax.inject.Inject

class EditSubnetViewModel @Inject constructor(
    private val meshNetworkManager: MeshNetworkManager

) : ViewModel() {
    private val isRemoveSubnetSucceed = MutableLiveData<Boolean>()


    fun getRemoveSubnetStatus() : LiveData<Boolean> {
        return isRemoveSubnetSucceed
    }


    fun updateSubnet(subnet: Subnet, newName: String) {
        if (!AppUtil.isNameValid(newName)) {
            return
        }

        try {
            subnet.name = newName
        } catch (e: SubnetChangeNameException) {
            Timber.e("updateSubnet exception: $e")
        }
    }

    fun removeSubnet(subnetToRemove: Subnet){
        Timber.d("removeSubnet")
        meshNetworkManager.removeSubnet(subnetToRemove,removeSubnetCallback)
    }

    private val removeSubnetCallback  = object : MeshNetworkManager.RemoveSubnetCallback {
        override fun success() {
            Timber.d("removeSubnetCallback: success")
            isRemoveSubnetSucceed.value = true
        }

        override fun error(subnet: Subnet?, error: ErrorType?) {
            Timber.e("removeSubnetCallback: error: ${subnet!!.name} --- $error")
            isRemoveSubnetSucceed.value = false
        }
    }
}