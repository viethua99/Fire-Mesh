package com.ceslab.firemesh.presentation.provision_list.dialog


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNodeManager
import com.ceslab.firemesh.meshmodule.model.ConnectableDeviceDescription
import com.ceslab.firemesh.meshmodule.model.MeshConnectableDevice
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.connectable_device.ConnectableDevice
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import com.siliconlab.bluetoothmesh.adk.provisioning.ProvisionerConnection
import com.siliconlab.bluetoothmesh.adk.provisioning.ProvisioningCallback
import timber.log.Timber
import javax.inject.Inject

class ProvisionDialogViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager, private val meshNodeManager: MeshNodeManager) : ViewModel() {
    private val subnetList = bluetoothMeshManager.currentNetwork?.subnets!!.sortedBy { it.name }
    private var selectedDeviceDescription: ConnectableDeviceDescription? = null
    private var provisionedDeviceName: String = ""
    private var subnetInfo : Subnet? = null

    val isProvisioningSucceed = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<ErrorType>()

    fun getNetworkNameList(): List<String> {
        Timber.d("getNetworkNameList")
        return subnetList.map { it.name }
    }

    fun provisionDevice(meshDeviceDescription: ConnectableDeviceDescription, spinnerIndex: Int,deviceName:String) {
        Timber.d("provisionDevice:${meshDeviceDescription.deviceAddress} ---$spinnerIndex")
        provisionedDeviceName = deviceName
        selectedDeviceDescription = meshDeviceDescription
        bluetoothMeshManager.currentSubnet = subnetList[spinnerIndex]
        this.subnetInfo = subnetList[spinnerIndex]
        val connectableDevice = selectedDeviceDescription!!.meshConnectableDevice!!
        if (subnetList.isNotEmpty()) {
            if (checkIfSelectedDeviceAlreadyAdded()) {
                val node = selectedDeviceDescription!!.existedNode
                if (node != null) {
                    node.removeOnlyFromLocalStructure()
                    startProvision(connectableDevice, subnetList[spinnerIndex])
                }
            } else {
                startProvision(connectableDevice, subnetList[spinnerIndex])
            }
        }
    }

    private fun startProvision(meshConnectableDevice: MeshConnectableDevice?, subnet: Subnet?) {
        Timber.d("provisionDevice")
        ProvisionerConnection(
            meshConnectableDevice,
            subnet
        ).provision(null, null, onProvisioningCallback)
    }

    private fun checkIfSelectedDeviceAlreadyAdded(): Boolean {
        Timber.d("checkIfSelectedDeviceAlreadyAdded")
        val connectableDevice = selectedDeviceDescription!!.meshConnectableDevice
        bluetoothMeshManager.currentNetwork?.let {
            it.subnets.forEach {
                it.nodes.forEach {
                    if (it.uuid != null && connectableDevice?.uuid != null) {
                        if (it.uuid!!.contentEquals(connectableDevice.uuid)) {
                            selectedDeviceDescription!!.existedNode = it
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    private val onProvisioningCallback = object : ProvisioningCallback {
        override fun success(connectableDevice: ConnectableDevice?, subnet: Subnet?, node: Node) {
            Timber.d("success: node=${node.name}")

            node.name = provisionedDeviceName
            bluetoothMeshManager.provisionedMeshConnectableDevice = selectedDeviceDescription!!.meshConnectableDevice
            bluetoothMeshManager.meshNodeToConfigure = meshNodeManager.getMeshNode(node)
            bluetoothMeshManager.currentSubnet = subnetInfo
            bluetoothMeshManager.currentNetwork = subnetInfo!!.network
            isProvisioningSucceed.value = true
        }

        override fun error(connectableDevice: ConnectableDevice?, subnet: Subnet?, error: ErrorType){
            Timber.d("error: $error")
            errorMessage.value = error
        }
    }
}