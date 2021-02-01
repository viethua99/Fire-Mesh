package com.ceslab.firemesh.presentation.group_list

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_group_list.*
import timber.log.Timber

class GroupListFragment : BaseFragment(){
    companion object {
        const val TAG = "GroupListFragment"
    }

    private lateinit var groupListRecyclerViewAdapter: GroupListRecyclerViewAdapter

    override fun getResLayoutId(): Int {
       return R.layout.fragment_group_list
    }

    override fun onMyViewCreated(view: View) {
       Timber.d("onMyViewCreated")
        setupRecyclerView()
    }

    private fun setupRecyclerView(){
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        groupListRecyclerViewAdapter = GroupListRecyclerViewAdapter(view!!.context)
        rv_group_list.layoutManager = linearLayoutManager
        rv_group_list.setHasFixedSize(true)
        rv_group_list.adapter = groupListRecyclerViewAdapter

        groupListRecyclerViewAdapter.setDataList(mutableListOf("A","B"))
    }
}