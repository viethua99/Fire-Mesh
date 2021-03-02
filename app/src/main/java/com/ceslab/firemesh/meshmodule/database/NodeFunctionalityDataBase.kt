package com.ceslab.firemesh.meshmodule.database

import android.content.Context
import android.content.SharedPreferences
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node

/**
 * Created by Viet Hua on 03/02/2021.
 */

class NodeFunctionalityDataBase(val context: Context) {
    private val FILE_NAME = "nodeFunctionality"
    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    fun getFunctionality(node: Node): NodeFunctionality.VENDOR_FUNCTIONALITY {
        val funcName = sharedPreferences.getString(node.uuid!!.contentToString(), NodeFunctionality.VENDOR_FUNCTIONALITY.Unknown.name)
        val functionality = NodeFunctionality.VENDOR_FUNCTIONALITY.values().find { it.name == funcName }
        return functionality ?: NodeFunctionality.VENDOR_FUNCTIONALITY.Unknown
    }

    fun saveFunctionality(node: Node, func: NodeFunctionality.VENDOR_FUNCTIONALITY) {
        val editor = sharedPreferences.edit()
        editor.putString(node.uuid!!.contentToString(), func.name)
        editor.apply()
    }

    fun removeFunctionality(node: Node) {
        val editor = sharedPreferences.edit()
        editor.remove(node.uuid!!.contentToString())
        editor.apply()
    }


}