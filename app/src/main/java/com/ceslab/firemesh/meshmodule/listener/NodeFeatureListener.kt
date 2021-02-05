package com.ceslab.firemesh.meshmodule.listener

interface NodeFeatureListener {
    fun onRelayStatusChanged(isEnabled:Boolean)
    fun onProxyStatusChanged(isEnabled:Boolean)
    fun onFriendStatusChanged(isEnabled:Boolean)

}