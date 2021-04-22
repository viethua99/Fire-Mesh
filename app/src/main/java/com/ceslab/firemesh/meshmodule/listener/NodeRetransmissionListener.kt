package com.ceslab.firemesh.meshmodule.listener

import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node

interface NodeRetransmissionListener {
    fun success(isEnabled:Boolean)
    fun error(node: Node?, error: ErrorType?)
}