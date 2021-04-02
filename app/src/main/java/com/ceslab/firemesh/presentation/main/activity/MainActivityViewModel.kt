package com.ceslab.firemesh.presentation.main.activity


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothle.BluetoothStateReceiver
import com.ceslab.firemesh.meshmodule.bluetoothle.LocationStateReceiver
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.siliconlab.bluetoothmesh.adk.data_model.key.NetKey
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import timber.log.Timber

import javax.inject.Inject

class MainActivityViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshNetworkManager: MeshNetworkManager,
    bluetoothStateReceiver: BluetoothStateReceiver,
    locationStateReceiver: LocationStateReceiver
) : ViewModel(), BluetoothStateReceiver.BluetoothStateListener,
    LocationStateReceiver.LocationStateListener {

    private val meshStatus = MutableLiveData<MeshStatus>()
    init {
        bluetoothStateReceiver.addListener(this)
        locationStateReceiver.addListener(this)
    }

    override fun onBluetoothStateChanged(enabled: Boolean) {
        Timber.d("onBluetoothStateChanged: $enabled")
        meshStatus.value = MeshStatus.BLUETOOTH_STATE_CHANGED
    }

    override fun onLocationStateChanged() {
        Timber.d("onLocationStateChanged")
        meshStatus.value = MeshStatus.LOCATION_STATE_CHANGED
    }

    fun getMeshStatus() :  LiveData<MeshStatus> {
        return this.meshStatus
    }

    fun setCurrentSubnet(netKey: ByteArray): Subnet?{
        val network = meshNetworkManager.network
        for (subnet in network!!.subnets) {
           if(subnet.netKey.key!!.contentEquals(netKey)){
                bluetoothMeshManager.currentSubnet = subnet
               return subnet
           }
        }
        return null
    }
}