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


    fun getSubnetList() : LiveData<Set<Subnet>>{
        subnetList.value = meshNetworkManager.network?.subnets!!
        return subnetList
    }



    fun setCurrentSubnet(subnet: Subnet) {
        Timber.d("setCurrentSubnet")
        bluetoothMeshManager.currentSubnet = subnet
    }

}