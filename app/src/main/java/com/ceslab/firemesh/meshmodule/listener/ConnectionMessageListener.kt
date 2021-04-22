package com.ceslab.firemesh.meshmodule.listener

import com.siliconlab.bluetoothmesh.adk.ErrorType

interface ConnectionMessageListener {
    fun connectionMessage(messageType: MessageType)

    fun connectionErrorMessage(error: ErrorType)

    enum class MessageType {
        NO_NODE_IN_SUBNET,

        GATT_NOT_CONNECTED,
        GATT_PROXY_DISCONNECTED,
        GATT_ERROR_DISCOVERING_SERVICES,

        PROXY_SERVICE_NOT_FOUND,
        PROXY_CHARACTERISTIC_NOT_FOUND,
        PROXY_DESCRIPTOR_NOT_FOUND,

        CONNECTING_TO_SUBNET,
        CONNECTING_TO_SUBNET_ERROR,
        REMOVING_SUBNET,
        REMOVING_SUBNET_ERROR
    }
}