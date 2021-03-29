package com.ceslab.firemesh.presentation.subnet_list.dialog.edit_subnet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConnectionManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNodeManager
import com.ceslab.firemesh.meshmodule.listener.ConnectionMessageListener
import com.ceslab.firemesh.meshmodule.listener.ConnectionStatusListener
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.ceslab.firemesh.util.AppUtil
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetChangeNameException
import timber.log.Timber
import javax.inject.Inject

class EditSubnetViewModel @Inject constructor(
    private val meshNetworkManager: MeshNetworkManager,
    private val meshConnectionManager: MeshConnectionManager,
    private val meshNodeManager: MeshNodeManager
) : ViewModel(), ConnectionStatusListener, ConnectionMessageListener {
    private val isRemoveSubnetSucceed = MutableLiveData<Boolean>()
    private val meshStatus = MutableLiveData<MeshStatus>()
    private val connectionMessage = MutableLiveData<ConnectionMessageListener.MessageType>()
    private lateinit var subnetToRemove: Subnet
    fun getMeshStatus(): LiveData<MeshStatus> {
        return meshStatus
    }

    fun getConnectionMessage(): LiveData<ConnectionMessageListener.MessageType> {
        return connectionMessage
    }

    fun getRemoveSubnetStatus(): LiveData<Boolean> {
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

    fun removeSubnet(subnetToRemove: Subnet) {
        Timber.d("removeSubnet")
        if (subnetToRemove.nodes.isEmpty()) {
            meshNetworkManager.removeSubnet(subnetToRemove, removeSubnetCallback)
        } else {
            removeSubnetWithNodes(subnetToRemove)
        }
    }

    fun deleteSubnetLocally(subnet: Subnet) {
        subnet.removeOnlyFromLocalStructure()
    }

    override fun connecting() {
        Timber.d("connecting")
        meshStatus.value = MeshStatus.MESH_CONNECTING
    }

    override fun connected() {
        Timber.d("connected")
        connectionMessage.value = ConnectionMessageListener.MessageType.REMOVING_SUBNET
        meshNetworkManager.removeSubnet(subnetToRemove,
            object : MeshNetworkManager.RemoveSubnetCallback {
                override fun success() {
                    Timber.d("removeSubnetCallback: success")
                    meshConnectionManager.disconnect()
                    removeNodesFunc(subnetToRemove)
                    isRemoveSubnetSucceed.value = true
                }

                override fun error(subnet: Subnet?, error: ErrorType?) {
                    Timber.e("removeSubnetCallback: error: ${subnet!!.name} --- $error")
                    isRemoveSubnetSucceed.value = false
                    clear()

                }
            })
        meshConnectionManager.removeMeshConnectionListener(this)
        meshConnectionManager.removeMeshMessageListener(this)

    }

    override fun disconnected() {
        Timber.d("disconnected")
        meshStatus.value = MeshStatus.MESH_DISCONNECTED
    }

    override fun connectionMessage(messageType: ConnectionMessageListener.MessageType) {
        Timber.d("connectionMessage: $messageType")
        connectionMessage.value = ConnectionMessageListener.MessageType.CONNECTING_TO_SUBNET_ERROR
        clear()
    }

    override fun connectionErrorMessage(error: ErrorType) {
        Timber.e("connectionErrorMessage: $error")
        isRemoveSubnetSucceed.value = false
        clear()
    }

    private fun clear() {
        meshConnectionManager.disconnect()
        meshConnectionManager.removeMeshMessageListener(this)
        meshConnectionManager.removeMeshConnectionListener(this)
    }

    private fun removeSubnetWithNodes(subnetToRemove: Subnet) {
        Timber.d("removeSubnetWithNodes")
        this.subnetToRemove = subnetToRemove
        meshConnectionManager.addMeshMessageListener(this)
        meshConnectionManager.addMeshConnectionListener(this)
        meshConnectionManager.connect(subnetToRemove)
    }

    private fun removeNodesFunc(subnet: Subnet) {
        meshNodeManager.getMeshNodeList(subnet).forEach {
            meshNodeManager.clearNodeFunctionalityList(it)
        }
    }

    private val removeSubnetCallback = object : MeshNetworkManager.RemoveSubnetCallback {
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