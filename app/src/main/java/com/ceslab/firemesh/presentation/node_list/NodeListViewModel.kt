package com.ceslab.firemesh.presentation.node_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.model.MeshNode
import javax.inject.Inject

class NodeListViewModel @Inject constructor() : ViewModel(){
    private val meshNodeList = MutableLiveData<Set<MeshNode>>()

    fun getMeshNodeList(): LiveData<Set<MeshNode>> {
        return meshNodeList
    }
}