package com.ceslab.firemesh.meshmodule.database

import android.content.Context
import android.content.SharedPreferences
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node
import timber.log.Timber
import java.lang.reflect.Type

/**
 * Created by Viet Hua on 03/02/2021.
 */

class NodeFunctionalityDataBase(val context: Context) {
    private val FILE_NAME = "nodeFunctionality"
    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    fun saveFunctionalityList(node: Node,functionalityList:Set<NodeFunctionality.VENDOR_FUNCTIONALITY>){
        Timber.d("saveFunctionalityList: ${node.uuid!!.contentToString()}")
        val gson = Gson()
        val json = gson.toJson(functionalityList)
        val editor = sharedPreferences.edit()
        editor.putString(node.uuid!!.contentToString(),json)
        editor.apply()
    }

    fun getFunctionalityList(node: Node):Set<NodeFunctionality.VENDOR_FUNCTIONALITY>{
        val gson = Gson()
        val json = sharedPreferences.getString(node.uuid!!.contentToString(),"")
        Timber.d("json=$json")
        val type = object : TypeToken<Set<NodeFunctionality.VENDOR_FUNCTIONALITY>>(){}.type
        val functionalityList = gson.fromJson<Set<NodeFunctionality.VENDOR_FUNCTIONALITY>>(json,type)
        return functionalityList ?: emptySet()
    }

    fun clearFunctionalityList(node: Node) {
        Timber.d("removeFunctionalityList: ${node.uuid!!.contentToString()}")
        val editor = sharedPreferences.edit()
        editor.remove(node.uuid!!.contentToString())
        editor.apply()
    }


}