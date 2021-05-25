package com.ceslab.firemesh.meshmodule.model

import com.siliconlab.bluetoothmesh.adk.data_model.node.Node

/**
 * Created by Viet Hua on 11/23/2020.
 */

data class MeshNode(val node: Node) {
    var fireSignal = 0
    var batteryPercent = 0
    var heartBeat = 0
    var gatewayType = GatewayType.NOT_GATEWAY
    var functionalityList = mutableSetOf<NodeFunctionality.VENDOR_FUNCTIONALITY>()

    enum class GatewayType {

        MAIN_GATEWAY,
        BACKUP_GATEWAY,
        NOT_GATEWAY
    }


    fun refresh() {
        fireSignal = 0
    }
}