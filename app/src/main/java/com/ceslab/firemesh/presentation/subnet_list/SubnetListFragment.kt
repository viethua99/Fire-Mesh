package com.ceslab.firemesh.presentation.subnet_list
import android.view.View
import androidx.core.view.ViewCompat

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.ota.utils.Converters
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.subnet.SubnetFragment
import com.ceslab.firemesh.presentation.subnet_list.dialog.add_subnet.AddSubnetClickListener
import com.ceslab.firemesh.presentation.subnet_list.dialog.add_subnet.AddSubnetDialog
import com.ceslab.firemesh.presentation.provision_list.SubnetListRecyclerViewAdapter
import com.ceslab.firemesh.presentation.subnet_list.dialog.edit_subnet.EditSubnetCallback
import com.ceslab.firemesh.presentation.subnet_list.dialog.edit_subnet.EditSubnetDialog
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_subnet_list.*
import timber.log.Timber

class SubnetListFragment : BaseFragment(){
    companion object {
        const val TAG = "SubnetListFragment"
    }

    private lateinit var subnetListRecyclerViewAdapter: SubnetListRecyclerViewAdapter
    private lateinit var subnetListViewModel: SubnetListViewModel

    override fun getResLayoutId(): Int {
        return R.layout.fragment_subnet_list
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


    private fun setupRecyclerView(){
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        subnetListRecyclerViewAdapter = SubnetListRecyclerViewAdapter(view!!.context)
        subnetListRecyclerViewAdapter.itemClickListener = onSubnetItemClickedListener
        rv_subnet_list.apply {
            layoutManager = linearLayoutManager
            setHasFixedSize(true)
            adapter = subnetListRecyclerViewAdapter
        }

    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        subnetListViewModel = ViewModelProvider(this, viewModelFactory).get(SubnetListViewModel::class.java)
        subnetListViewModel.apply {
            getSubnetList().observe(this@SubnetListFragment,subnetListObserver)
        }

    }

    private fun setupAddGroupFab() {
        Timber.d("setupAddGroupFab")
        fab_add_subnet.setOnClickListener {
            Timber.d("onAddGroupClick")
            val addSubnetDialog = AddSubnetDialog()
            addSubnetDialog.show(fragmentManager!!, "AddSubnetDialog")
            addSubnetDialog.setAddSubnetClickListener(onAddSubnetClickListener)
        }
    }

    private val onSubnetItemClickedListener = object : BaseRecyclerViewAdapter.ItemClickListener<Subnet> {
        override fun onClick(position: Int, item: Subnet) {
            Timber.d("onSubnetItemClickedListener: clicked")
            ViewCompat.postOnAnimationDelayed(view!!, // Delay to show ripple effect
                Runnable {
                    subnetListViewModel.setCurrentSubnet(item)
                    val mainActivity = activity as MainActivity
                    mainActivity.replaceFragment(SubnetFragment(item.name),SubnetFragment.TAG,R.id.container_main)
                }
                ,50)
        }

        override fun onLongClick(position: Int, item: Subnet) {
            Timber.d("onSubnetItemClickedListener: longClicked")
            val editSubnetDialog = EditSubnetDialog(item)
            editSubnetDialog.show(fragmentManager!!, "EditSubnetDialog")
            editSubnetDialog.setEditSubnetCallback(onEditSubnetCallback)

        }
    }

    private val subnetListObserver = Observer<Set<Subnet>> {
        activity?.runOnUiThread {
            if (it.isNotEmpty()) {
                no_subnet_background.visibility = View.GONE
                subnetListRecyclerViewAdapter.setDataList(it.toMutableList())
            }  else {
                subnetListRecyclerViewAdapter.clear()
                no_subnet_background.visibility = View.VISIBLE
            }
        }
    }

    private val onAddSubnetClickListener = object :
        AddSubnetClickListener {
        override fun onClicked() {
            subnetListViewModel.getSubnetList()
        }
    }

    private val onEditSubnetCallback = object : EditSubnetCallback {
        override fun onChanged() {
            subnetListViewModel.getSubnetList()

        }
    }

}