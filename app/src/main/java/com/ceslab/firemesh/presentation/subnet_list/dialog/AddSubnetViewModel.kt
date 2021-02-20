package com.ceslab.firemesh.presentation.subnet_list.dialog

import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.util.AppUtil
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetCreationException
import timber.log.Timber
import javax.inject.Inject

class AddSubnetViewModel @Inject constructor(
    private val meshNetworkManager: MeshNetworkManager

) : ViewModel() {

    fun addSubnet(newSubnetName: String){
        Timber.d("addSubnet: $newSubnetName")
        if (!AppUtil.isNameValid(newSubnetName)) {
            return
        }
        try {
            meshNetworkManager.createSubnet(newSubnetName)
        } catch (e: SubnetCreationException) {
            Timber.e("addSubnet exception: $e")
        }

    }
}