package com.ceslab.firemesh.meshmodule.bluetoothmesh

import com.ceslab.firemesh.meshmodule.database.NodeFunctionalityDataBase
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import timber.log.Timber

/**
 * Created by Viet Hua on 11/23/2020.
 */

class MeshNodeManager(private val nodeFunctionalityDb: NodeFunctionalityDataBase) {
    private val meshNodes = mutableMapOf<Node, MeshNode>()

    fun getMeshNode(node: Node): MeshNode {
        return wrapNode(node)
    }

    fun getMeshNodeList(subnet: Subnet): Set<MeshNode> {
        return wrapNodeList(subnet.nodes)
    }

    fun getMeshNodeList(group: Group): Set<MeshNode> {
        return wrapNodeList(group.nodes)
    }

    private fun wrapNode(node: Node): MeshNode {
        var meshNode: MeshNode? = meshNodes[node]
        if (meshNode == null) {
            Timber.d("meshNode = null")
            meshNode = MeshNode(node)
            meshNode.functionalityList = nodeFunctionalityDb.getFunctionalityList(node).toMutableSet()
            meshNodes[node] = meshNode
        }
        Timber.d("wrapNode = ${meshNode.functionalityList}")
        return meshNode
    }

    private fun wrapNodeList(nodes: Set<Node>): Set<MeshNode> {
        val result = mutableSetOf<MeshNode>()
        nodes.forEach { node ->
            result.add(wrapNode(node))
        }
        return result
    }

    fun addNodeFunctionalityToList(meshNode: MeshNode, functionality: NodeFunctionality.VENDOR_FUNCTIONALITY) {
        Timber.d("updateNodeFunc: ${functionality.name}")
        if (functionality != NodeFunctionality.VENDOR_FUNCTIONALITY.Unknown) {
            meshNode.functionalityList.add(functionality)
            nodeFunctionalityDb.saveFunctionalityList(meshNode.node, meshNode.functionalityList)
        }
    }

    fun removeNodeFunctionalityFromList(meshNode: MeshNode, functionality: NodeFunctionality.VENDOR_FUNCTIONALITY){
        Timber.d("removeNodeFunctionalityFromList: ${functionality.name}")
        if(meshNode.functionalityList.contains(functionality)){
            meshNode.functionalityList.remove(functionality)
            nodeFunctionalityDb.saveFunctionalityList(meshNode.node,meshNode.functionalityList)
        }
    }

    fun clearNodeFunctionalityList(meshNode: MeshNode) {
        Timber.d("clearNodeFunctionalityList")
        meshNode.functionalityList = mutableSetOf()
        nodeFunctionalityDb.clearFunctionalityList(meshNode.node)
    }
}