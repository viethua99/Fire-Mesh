package com.ceslab.firemesh.presentation.node.node_config

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConfigurationManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConnectionManager
import com.ceslab.firemesh.meshmodule.listener.ConfigurationStatusListener
import com.ceslab.firemesh.meshmodule.listener.MeshLoadedListener
import com.ceslab.firemesh.meshmodule.listener.NodeFeatureListener
import com.ceslab.firemesh.meshmodule.model.ConfigurationStatus
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import timber.log.Timber
import javax.inject.Inject

class NodeConfigViewModel @Inject constructor(
    private val meshConfigurationManager: MeshConfigurationManager,
    private val meshConnectionManager: MeshConnectionManager
): ViewModel(){

    private val meshNodeToConfigure = MutableLiveData<MeshNode>()
    private val proxyStatus = MutableLiveData<Boolean>()
    private val relayStatus = MutableLiveData<Boolean>()
    private val friendStatus = MutableLiveData<Boolean>()
    private val configurationStatus = MutableLiveData<ConfigurationStatus>()


    fun getMeshNodeToConfigure(): LiveData<MeshNode> {
        return meshNodeToConfigure
    }

    fun getRelayStatus(): LiveData<Boolean> {
        return relayStatus
    }
    fun getProxyStatus(): LiveData<Boolean> {
        return proxyStatus
    }
    fun getFriendStatus(): LiveData<Boolean> {
        return friendStatus
    }

    fun getConfigurationStatus() : LiveData<ConfigurationStatus> {
        return configurationStatus
    }


    fun setConfigListeners(){
        Timber.d("setListeners")
        meshConnectionManager.removeMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
        meshConfigurationManager.apply {
            addNodeFeatureListener(nodeFeatureListener)
            addConfigurationStatusListener(configurationStatusListener)
        }
    }

    fun removeConfigListeners() {
        Timber.d("removeMeshConnectionListener")
        meshConnectionManager.removeMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
        meshConfigurationManager.apply {
            removeConfigurationStatusListener(configurationStatusListener)
            removeNodeFeatureListener(nodeFeatureListener)
        }
    }

    fun changeGroup(newGroup: Group?)  {
        Timber.d("changeGroup: $newGroup")
        meshConfigurationManager.processChangeGroup(newGroup)
    }

    fun changeFunctionality(newFunctionality: NodeFunctionality.VENDOR_FUNCTIONALITY) {
        Timber.d("changeFunctionality: $newFunctionality")
        meshConfigurationManager.processChangeFunctionality(newFunctionality)
    }

    fun changeRelay(enabled:Boolean) {
        Timber.d("changeRelay: $enabled")
        meshConfigurationManager.changeRelay(enabled)
    }

    fun changeProxy(enabled:Boolean) {
        Timber.d("changeProxy: $enabled")
        meshConfigurationManager.changeProxy(enabled)

    }

    fun changeFriend(enabled:Boolean) {
        Timber.d("changeFriend: $enabled")
        meshConfigurationManager.changeFriend(enabled)

    }


    private val meshConfigurationLoadedListener = object : MeshLoadedListener {
        override fun initialConfigurationLoaded() {
            Timber.d("initialConfigurationLoaded")
            meshConfigurationManager.apply {
                checkFriendStatus()
                checkRelayStatus()
                checkProxyStatus()
            }
        }
    }

    private val nodeFeatureListener = object :NodeFeatureListener {
        override fun onRelayStatusChanged(isEnabled: Boolean) {
            Timber.d("onRelayStatusChanged : $isEnabled")
            relayStatus.value = isEnabled
        }

        override fun onProxyStatusChanged(isEnabled: Boolean) {
            Timber.d("onProxyStatusChanged : $isEnabled")
            proxyStatus.value = isEnabled

        }

        override fun onFriendStatusChanged(isEnabled: Boolean) {
            Timber.d("onFriendStatusChanged : $isEnabled")
            friendStatus.value = isEnabled
        }
    }

    private val configurationStatusListener = object : ConfigurationStatusListener {
        override fun onConfigurationStatusChanged(configurationStatus: ConfigurationStatus) {
            Timber.d("onConfigurationStatusChanged : $configurationStatus")
            this@NodeConfigViewModel.configurationStatus.value = configurationStatus
        }
    }
}