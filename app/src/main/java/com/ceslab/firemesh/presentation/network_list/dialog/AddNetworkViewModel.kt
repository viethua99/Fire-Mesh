package com.ceslab.firemesh.presentation.network_list.dialog

import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.util.AppUtil
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetCreationException
import timber.log.Timber
import javax.inject.Inject

class AddNetworkViewModel @Inject constructor(
    private val meshNetworkManager: MeshNetworkManager

) : ViewModel() {

    fun addNetwork(newNetworkName: String){
        Timber.d("addGroup: $newNetworkName")
        if (!AppUtil.isNameValid(newNetworkName)) {
            return
        }
        try {
            meshNetworkManager.createSubnet(newNetworkName)
        } catch (e: SubnetCreationException) {
            Timber.e("addNetwork exception: $e")
        }

    }
}