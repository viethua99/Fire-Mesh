package com.ceslab.firemesh.presentation.subnet_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import timber.log.Timber
import javax.inject.Inject

class SubnetListViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshNetworkManager: MeshNetworkManager
) : ViewModel(){
    private val subnetList = MutableLiveData<Set<Subnet>>()
    private val isRemoveSubnetSucceed = MutableLiveData<Boolean>()

    fun getSubnetList() : LiveData<Set<Subnet>>{
        subnetList.value = meshNetworkManager.network?.subnets!!
        return subnetList
    }

    fun getRemoveSubnetStatus() : LiveData<Boolean> {
        return isRemoveSubnetSucceed
    }


    fun setCurrentSubnet(subnet: Subnet) {
        Timber.d("setCurrentSubnet")
        bluetoothMeshManager.currentSubnet = subnet
    }

    fun removeSubnet(subnetToRemove:Subnet){
        Timber.d("removeSubnet")
        meshNetworkManager.removeSubnet(subnetToRemove,removeSubnetCallback)
    }

    private val removeSubnetCallback  = object : MeshNetworkManager.RemoveSubnetCallback {
        override fun success() {
            Timber.d("removeSubnetCallback: success")
            subnetList.value = meshNetworkManager.network?.subnets!!
            isRemoveSubnetSucceed.value = true
        }

        override fun error(subnet: Subnet?, error: ErrorType?) {
            Timber.e("removeSubnetCallback: error: ${subnet!!.name} --- $error")
            isRemoveSubnetSucceed.value = false
        }
    }
}