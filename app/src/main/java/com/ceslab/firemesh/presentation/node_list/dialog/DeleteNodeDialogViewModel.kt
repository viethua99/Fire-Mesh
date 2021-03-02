package com.ceslab.firemesh.presentation.node_list.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNodeManager
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.configuration_control.ConfigurationControl
import com.siliconlab.bluetoothmesh.adk.configuration_control.FactoryResetCallback
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Viet Hua on 03/02/2021.
 */

class DeleteNodeDialogViewModel @Inject constructor(
   private val meshNodeManager: MeshNodeManager
) : ViewModel() {

    private val isDeletedSucceed = MutableLiveData<Boolean>()

    fun deleteNode(meshNode: MeshNode) {
        Timber.d("deleteNode: ${meshNode.node.name}")
        val configurationControl = ConfigurationControl(meshNode.node)
        factoryResetDevice(configurationControl)
    }

    fun getDeleteNodeStatus() : LiveData<Boolean> {
        return isDeletedSucceed
    }

    private fun factoryResetDevice(configurationControl: ConfigurationControl) {
        configurationControl.factoryReset(object : FactoryResetCallback {
            override fun success(node: Node?) {
                node?.let {
                    val meshNode = meshNodeManager.getMeshNode(it)
                    meshNodeManager.removeNodeFunc(meshNode)
                    isDeletedSucceed.value = true
                }
            }

            override fun error(node: Node?, error: ErrorType?) {
                Timber.d("error: ${error!!.type}")
                isDeletedSucceed.value = false
            }
        })
    }

     fun deleteDeviceLocally(node: Node) {
         Timber.d("deleteDeviceLocally: ${node.name}")
        node.removeOnlyFromLocalStructure()
    }
}