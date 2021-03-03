package com.ceslab.firemesh.presentation.node.node_config

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConfigurationManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConnectionManager
import com.ceslab.firemesh.meshmodule.listener.ConfigurationTaskListener
import com.ceslab.firemesh.meshmodule.listener.ConnectionStatusListener
import com.ceslab.firemesh.meshmodule.listener.MeshLoadedListener
import com.ceslab.firemesh.meshmodule.listener.NodeFeatureListener
import com.ceslab.firemesh.meshmodule.model.*
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node
import timber.log.Timber
import javax.inject.Inject

class NodeConfigViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshConfigurationManager: MeshConfigurationManager,
    private val meshConnectionManager: MeshConnectionManager
) : ViewModel() {
    private var getDeviceConfigRequested = false
    private var isSupportedProxy: Boolean? = null
    private var isSupportedRelay: Boolean? = null
    private var isSupportedFriend: Boolean? = null
    private var isSupportedLowPower: Boolean? = null


    private var isProxyEnabled = MutableLiveData<Boolean>()
    private var isFriendEnabled = MutableLiveData<Boolean>()
    private var isRelayEnabled = MutableLiveData<Boolean>()
    private var nodeConfig = MutableLiveData<NodeConfig>()
    private val currentConfigTask = MutableLiveData<ConfigurationTask>()
    private val configurationError = MutableLiveData<ErrorType>()


    fun getProxyStatus(): LiveData<Boolean> {
        return isProxyEnabled
    }

    fun getRelayStatus(): LiveData<Boolean> {
        return isRelayEnabled
    }

    fun getFriendStatus(): LiveData<Boolean> {
        return isFriendEnabled
    }

    fun getCurrentConfigTask(): LiveData<ConfigurationTask> {
        return currentConfigTask
    }

    fun getNodeConfig(): LiveData<NodeConfig> {
        return nodeConfig
    }

    fun getConfigurationError(): LiveData<ErrorType> {
        return configurationError
    }


    fun setConfigListeners() {
        Timber.d("setListeners")
        meshConnectionManager.apply {
            addMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
            addMeshConnectionListener(meshConnectionListener)
        }

        meshConfigurationManager.apply {
            addNodeFeatureListener(nodeFeatureListener)
            addConfigurationTaskListener(configurationTaskListener)
        }
    }

    fun removeConfigListeners() {
        Timber.d("removeMeshConnectionListener")
        meshConnectionManager.apply {
            removeMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
            removeMeshConnectionListener(meshConnectionListener)
        }

        meshConfigurationManager.apply {
            removeConfigurationTaskListener(configurationTaskListener)
            removeNodeFeatureListener(nodeFeatureListener)
        }
    }

    fun changeGroup(newGroup: Group?) {
        Timber.d("changeGroup: $newGroup")
        meshConfigurationManager.processChangeGroup(newGroup)
    }

    fun changeFunctionality(newFunctionality: NodeFunctionality.VENDOR_FUNCTIONALITY) {
        Timber.d("changeFunctionality: $newFunctionality")
        meshConfigurationManager.processChangeFunctionality(newFunctionality)
    }

    fun changeRelay(enabled: Boolean) {
        Timber.d("changeRelay: $enabled")
        meshConfigurationManager.changeRelay(enabled)
    }

    fun changeProxy(enabled: Boolean) {
        Timber.d("changeProxy: $enabled")
        meshConfigurationManager.changeProxy(enabled)

    }

    fun changeFriend(enabled: Boolean) {
        Timber.d("changeFriend: $enabled")
        meshConfigurationManager.changeFriend(enabled)
    }

    fun updateProxy() {
        Timber.d("updateProxy")
        meshConfigurationManager.checkProxyStatus()
    }

    fun updateFriend() {
        Timber.d("updateFriend")
        meshConfigurationManager.checkFriendStatus()
    }

    fun updateRelay() {
        Timber.d("updateRelay")
        meshConfigurationManager.checkRelayStatus()

    }

    private fun checkFeaturesStatus() {
        Timber.d("checkFeaturesStatus")
        bluetoothMeshManager.meshNodeToConfigure!!.node.deviceCompositionData?.apply {
            isSupportedLowPower = supportsLowPower()
            isSupportedProxy = supportsProxy()
            isSupportedFriend = supportsFriend()
            isSupportedRelay = supportsRelay()

            if (isSupportedProxy == true) {
                updateProxy()
            }

            if (isSupportedFriend == true) {
                updateFriend()
            }

            if (isSupportedRelay == true) {
                updateRelay()
            }
        }

    }

    private val meshConnectionListener = object : ConnectionStatusListener {
        override fun connecting() {}
        override fun disconnected() {}
        override fun connected() {
            Timber.d("connected")
            if (!getDeviceConfigRequested) {
                checkFeaturesStatus()
                meshConfigurationManager.startTasks()
                getDeviceConfigRequested = true
            }
        }
    }

    private val meshConfigurationLoadedListener = object : MeshLoadedListener {
        override fun initialConfigurationLoaded() {
            Timber.d("initialConfigurationLoaded")
            checkFeaturesStatus()
            meshConfigurationManager.startTasks()
        }
    }

    private val nodeFeatureListener = object : NodeFeatureListener {
        override fun onGetRelayStatusSucceed(isEnabled: Boolean) {
            Timber.d("onGetRelayStatusSucceed : $isEnabled")
            isRelayEnabled.value = isEnabled
        }

        override fun onGetProxyStatusSucceed(isEnabled: Boolean) {
            Timber.d("onGetProxyStatusSucceed : $isEnabled")
            isProxyEnabled.value = isEnabled
        }

        override fun onGetFriendStatusSucceed(isEnabled: Boolean) {
            Timber.d("onGetFriendStatusSucceed : $isEnabled")
            isFriendEnabled.value = isEnabled
        }

        override fun onSetNodeFeatureError(error: ErrorType) {
            Timber.e("onSetNodeFeatureError: ${error.type}")
        }
    }

    private val configurationTaskListener = object : ConfigurationTaskListener {
        override fun onCurrentConfigTask(configurationTask: ConfigurationTask) {
            Timber.d("onCurrentConfigTask : $configurationTask")
            this@NodeConfigViewModel.currentConfigTask.value = configurationTask
        }

        override fun onConfigError(errorType: ErrorType) {
            Timber.e("onConfigError : ${errorType.type}")
            configurationError.value = errorType
        }

        override fun onConfigFinish() {
            Timber.d("onConfigFinish")
         val  currentNodeConfig = NodeConfig(
                bluetoothMeshManager.meshNodeToConfigure!!,
                isSupportedLowPower,
                isSupportedRelay,
                isSupportedProxy,
                isSupportedFriend
            )

            nodeConfig.postValue(currentNodeConfig)
        }
    }


}