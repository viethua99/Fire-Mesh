package com.ceslab.firemesh.presentation.node_list

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.group_list.GroupListRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_group_list.*
import kotlinx.android.synthetic.main.fragment_node_list.*
import timber.log.Timber

class NodeListFragment : BaseFragment(){
    companion object {
        const val TAG = "NodeListFragment"
    }

    private lateinit var nodeListRecyclerViewAdapter: NodeListRecyclerViewAdapter

    override fun getResLayoutId(): Int {
       return R.layout.fragment_node_list
    }

    override fun onMyViewCreated(view: View) {
       Timber.d("onMyViewCreated")
        setupRecyclerView()
    }

    private fun setupRecyclerView(){
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        nodeListRecyclerViewAdapter = NodeListRecyclerViewAdapter(view!!.context)
        rv_node_list.layoutManager = linearLayoutManager
        rv_node_list.setHasFixedSize(true)
        rv_node_list.adapter = nodeListRecyclerViewAdapter
        nodeListRecyclerViewAdapter.setDataList(mutableListOf("A","B","C","D"))
    }
}