package com.ceslab.firemesh.meshmodule.listener

import com.ceslab.firemesh.meshmodule.model.ConfigurationStatus

interface ConfigurationStatusListener {
    fun onConfigurationStatusChanged(configurationStatus:ConfigurationStatus)
}