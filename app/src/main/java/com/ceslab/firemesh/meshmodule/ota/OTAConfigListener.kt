package com.ceslab.firemesh.meshmodule.ota

interface OTAConfigListener {
    fun onGattConnected()
    fun onGattDisconnected()
    fun onGattConnectFailed(status:Int)
}