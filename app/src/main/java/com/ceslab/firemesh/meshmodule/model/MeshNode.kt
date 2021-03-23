package com.ceslab.firemesh.meshmodule.model

import com.siliconlab.bluetoothmesh.adk.data_model.node.Node

/**
 * Created by Viet Hua on 11/23/2020.
 */

data class MeshNode(val node: Node) {
    var fireSignal = 0
    var functionality = NodeFunctionality.VENDOR_FUNCTIONALITY.Unknown
    var functionalityList = mutableListOf<NodeFunctionality.VENDOR_FUNCTIONALITY>()


    fun refresh() {
        fireSignal = 0
    }
}