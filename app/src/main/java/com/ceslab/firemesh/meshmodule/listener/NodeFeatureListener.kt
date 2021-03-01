package com.ceslab.firemesh.meshmodule.listener

import com.siliconlab.bluetoothmesh.adk.ErrorType

interface NodeFeatureListener {
    fun onGetRelayStatusSucceed(isEnabled:Boolean)
    fun onGetProxyStatusSucceed(isEnabled:Boolean)
    fun onGetFriendStatusSucceed(isEnabled:Boolean)
    fun onSetNodeFeatureError(error: ErrorType)
}