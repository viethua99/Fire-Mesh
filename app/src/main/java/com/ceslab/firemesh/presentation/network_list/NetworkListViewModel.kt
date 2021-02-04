package com.ceslab.firemesh.presentation.network_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import timber.log.Timber
import javax.inject.Inject

class NetworkListViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshNetworkManager: MeshNetworkManager
) : ViewModel(){
    private val networkList = MutableLiveData<Set<Subnet>>()

    init {
        networkList.value = meshNetworkManager.network?.subnets!!
    }

    fun getNetworkList() : LiveData<Set<Subnet>>{
        return networkList
    }

    fun setCurrentNetwork(subnet: Subnet) {
        Timber.d("setCurrentNetwork")
        bluetoothMeshManager.currentSubnet = subnet
    }
}