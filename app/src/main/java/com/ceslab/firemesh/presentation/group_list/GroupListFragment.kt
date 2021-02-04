package com.ceslab.firemesh.presentation.group_list

import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.network_list.NetworkListViewModel
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_group_list.*
import timber.log.Timber

class GroupListFragment : BaseFragment(){
    companion object {
        const val TAG = "GroupListFragment"
    }

    private lateinit var groupListRecyclerViewAdapter: GroupListRecyclerViewAdapter
    private lateinit var groupListViewModel: GroupListViewModel

    override fun getResLayoutId(): Int {
       return R.layout.fragment_group_list
    }

    override fun onMyViewCreated(view: View) {
       Timber.d("onMyViewCreated")
        setupViewModel()
        setupRecyclerView()
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        groupListViewModel = ViewModelProvider(this, viewModelFactory).get(GroupListViewModel::class.java)
        groupListViewModel.getGroupList().observe(this,groupListObserver)
    }

    private fun setupRecyclerView(){
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        groupListRecyclerViewAdapter = GroupListRecyclerViewAdapter(view!!.context)
        rv_group_list.layoutManager = linearLayoutManager
        rv_group_list.setHasFixedSize(true)
        rv_group_list.adapter = groupListRecyclerViewAdapter
    }

    private val groupListObserver = Observer<Set<Group>> {
        activity?.runOnUiThread {
            groupListRecyclerViewAdapter.setDataList(it.toMutableList())

        }
    }
}