package com.ceslab.firemesh.presentation.group_list

import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.group_list.dialog.AddGroupClickListener
import com.ceslab.firemesh.presentation.group_list.dialog.AddGroupDialog
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_group_list.*
import kotlinx.android.synthetic.main.fragment_subnet_list.*
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
        setupAddGroupFab()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
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

    private fun setupAddGroupFab() {
        Timber.d("setupAddGroupFab")
        fab_add_group.setOnClickListener {
            Timber.d("onAddGroupClick")
            val addGroupDialog = AddGroupDialog()
            addGroupDialog.show(fragmentManager!!, "AddGroupDialog")
            addGroupDialog.setAddGroupClickListener(onAddGroupClickListener)
        }
    }

    private val groupListObserver = Observer<Set<Group>> {
        activity?.runOnUiThread {
            if(it.isNotEmpty()) {
                no_group_background.visibility = View.GONE
                groupListRecyclerViewAdapter.setDataList(it.toMutableList())
            }
        }
    }

    private val onAddGroupClickListener = object : AddGroupClickListener {
        override fun onClicked() {
           groupListViewModel.getGroupList()
        }
    }
}