package com.ceslab.firemesh.presentation.node.node_config.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConfigurationManager
import com.ceslab.firemesh.meshmodule.listener.ConfigurationTaskListener
import com.ceslab.firemesh.meshmodule.model.ConfigurationTask
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.siliconlab.bluetoothmesh.adk.ErrorType
import timber.log.Timber

import javax.inject.Inject

class ModelConfigViewModel @Inject constructor(
    private val meshConfigurationManager: MeshConfigurationManager
    ) : ViewModel() {

    private var isConfigFinished = MutableLiveData<Boolean>()

    fun getConfigFinishStatus():LiveData<Boolean>{
        return isConfigFinished
    }
    fun bindFunctionality(newFunctionality: NodeFunctionality.VENDOR_FUNCTIONALITY, isSetPublication:Boolean, isAddSubscription: Boolean) {
        Timber.d("changeFunctionality: functionality=$newFunctionality -- publication=$isSetPublication --subscription=$isAddSubscription")
        meshConfigurationManager.addConfigurationTaskListener(configurationTaskListener)
        meshConfigurationManager.processBindModelToGroup(newFunctionality,isSetPublication,isAddSubscription)
    }

    private val configurationTaskListener = object : ConfigurationTaskListener {
        override fun onCurrentConfigTask(configurationTask: ConfigurationTask) {
            Timber.d("onCurrentConfigTask : $configurationTask")

        }

        override fun onConfigError(errorType: ErrorType) {
            Timber.e("onConfigError : ${errorType.type}")

        }

        override fun onConfigFinish() {
            Timber.d("onConfigFinish")
            isConfigFinished.postValue(true)
            meshConfigurationManager.removeConfigurationTaskListener(this)
        }
    }

}