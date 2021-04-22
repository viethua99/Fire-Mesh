package com.ceslab.firemesh.meshmodule.listener

import com.ceslab.firemesh.meshmodule.model.ConfigurationTask
import com.siliconlab.bluetoothmesh.adk.ErrorType

interface ConfigurationTaskListener {
    fun onConfigFinish()
    fun onCurrentConfigTask(configurationTask:ConfigurationTask)
    fun onConfigError(errorType: ErrorType)
}