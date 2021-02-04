package com.ceslab.firemesh.presentation.scan.dialog


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
    private val networkList = bluetoothMeshManager.currentNetwork?.subnets!!.sortedBy { it.name }
    private var selectedDeviceDescription: ConnectableDeviceDescription? = null

    val isProvisioningSucceed = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<ErrorType>()

    fun provisionDevice(meshDeviceDescription: ConnectableDeviceDescription, spinnerIndex: Int) {
        Timber.d("provisionDevice:${meshDeviceDescription.deviceAddress} ---$spinnerIndex")
        selectedDeviceDescription = meshDeviceDescription
        bluetoothMeshManager.currentSubnet = networkList[spinnerIndex]
        if (networkList.isNotEmpty()) {
            if (checkIfSelectedDeviceAlreadyAdded()) {
                val connectableDevice = selectedDeviceDescription!!.meshConnectableDevice!!
                val node = selectedDeviceDescription!!.existedNode
                if (node != null) {
                    node.removeOnlyFromLocalStructure()
                    startProvision(connectableDevice, bluetoothMeshManager.currentSubnet)
                }
            } else {
                startProvision(
                    meshDeviceDescription.meshConnectableDevice,
                    bluetoothMeshManager.currentSubnet
                )
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
            bluetoothMeshManager.meshNodeToConfigure = meshNodeManager.getMeshNode(node)
            bluetoothMeshManager.provisionedMeshConnectableDevice =
                selectedDeviceDescription!!.meshConnectableDevice
            isProvisioningSucceed.value = true
        }

        override fun error(connectableDevice: ConnectableDevice?, subnet: Subnet?, error: ErrorType){
            Timber.d("error: $error")
            errorMessage.value = error
        }
    }
}