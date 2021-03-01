package com.ceslab.firemesh.meshmodule.listener

import com.ceslab.firemesh.meshmodule.model.ConfigurationStatus
import com.siliconlab.bluetoothmesh.adk.ErrorType

interface ConfigurationStatusListener {
    fun onConfigurationStatusChanged(configurationStatus:ConfigurationStatus)
    fun onConfigurationError(errorType: ErrorType)
}