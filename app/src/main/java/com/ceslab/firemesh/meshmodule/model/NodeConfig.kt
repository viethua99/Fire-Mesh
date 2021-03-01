package com.ceslab.firemesh.meshmodule.model


data class NodeConfig(
    val meshNode: MeshNode,
    var isSupportLowPower: Boolean? = null,
    var isSupportRelay: Boolean? = null,
    var isSupportProxy: Boolean? = null,
    var isSupportFriend: Boolean? = null
)