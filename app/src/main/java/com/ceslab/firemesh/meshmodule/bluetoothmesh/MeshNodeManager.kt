package com.ceslab.firemesh.meshmodule.bluetoothmesh

import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet

/**
 * Created by Viet Hua on 11/23/2020.
 */

class MeshNodeManager {
    private val meshNodes = mutableMapOf<Node, MeshNode>()

    fun getMeshNode(node: Node): MeshNode {
        return wrapNode(node)
    }

    fun getMeshNodeList(subnet: Subnet):Set<MeshNode>{
        return wrapNodeList(subnet.nodes)
    }

    private fun wrapNode(node: Node): MeshNode {
        var meshNode: MeshNode? = meshNodes[node]
        if (meshNode == null) {
            meshNode = MeshNode(node)
            meshNode.functionality = NodeFunctionality.VENDOR_FUNCTIONALITY.Unknown
            meshNodes[node] = meshNode
        }
        return meshNode
    }

    private fun wrapNodeList(nodes: Set<Node>): Set<MeshNode> {
        val result = mutableSetOf<MeshNode>()
        nodes.forEach { node ->
            result.add(wrapNode(node))
        }
        return result
    }
}