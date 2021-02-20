package com.ceslab.firemesh.presentation.group_list

import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.group_list.dialog.AddGroupClickListener
import com.ceslab.firemesh.presentation.group_list.dialog.AddGroupDialog
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.subnet.SubnetFragment
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
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
    private lateinit var groupToRemove : Group

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
        groupListViewModel.apply {
            getGroupList().observe(this@GroupListFragment,groupListObserver)
            getRemoveGroupStatus().observe(this@GroupListFragment,isGroupRemoveSucceedObserver)
        }
    }

    private fun setupRecyclerView(){
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        groupListRecyclerViewAdapter = GroupListRecyclerViewAdapter(view!!.context)
        groupListRecyclerViewAdapter.itemClickListener = onGroupItemClickedListener

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

    private val onGroupItemClickedListener = object : BaseRecyclerViewAdapter.ItemClickListener<Group> {
        override fun onClick(position: Int, item: Group) {}

        override fun onLongClick(position: Int, item: Group) {
            Timber.d("onGroupItemClickedListener: longClicked")
            showWarningDialog("Do you want to delete group?")
            groupToRemove = item
            setOnConfirmDialogCLicked(sweetClickListener)
        }
    }

    private val groupListObserver = Observer<Set<Group>> {
        activity?.runOnUiThread {
            if(it.isNotEmpty()) {
                no_group_background.visibility = View.GONE
                groupListRecyclerViewAdapter.setDataList(it.toMutableList())
            } else {
                groupListRecyclerViewAdapter.clear()
                no_group_background.visibility = View.VISIBLE
            }
        }
    }

    private val isGroupRemoveSucceedObserver = Observer<Boolean> {
        activity?.runOnUiThread {
            hideDialog()
            if(it == true) {
                showSuccessDialog("Remove Succeed")
            } else {
                showFailedDialog("Remove Failed")
            }
        }
    }

    private val onAddGroupClickListener = object : AddGroupClickListener {
        override fun onClicked() {
           groupListViewModel.getGroupList()
        }
    }

    private val sweetClickListener = SweetAlertDialog.OnSweetClickListener {
        groupListViewModel.removeGroup(groupToRemove)
    }
}