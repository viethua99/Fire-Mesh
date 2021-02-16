package com.ceslab.firemesh.presentation.network_list
import android.view.View

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.network.NetworkFragment
import com.ceslab.firemesh.presentation.network_list.dialog.AddNetworkClickListener
import com.ceslab.firemesh.presentation.network_list.dialog.AddNetworkDialog
import com.ceslab.firemesh.presentation.scan.NetworkListRecyclerViewAdapter
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_network_list.*
import timber.log.Timber

class NetworkListFragment : BaseFragment(){
    companion object {
        const val TAG = "NetworkListFragment"
    }

    private lateinit var networkListRecyclerViewAdapter: NetworkListRecyclerViewAdapter
    private lateinit var networkListViewModel: NetworkListViewModel


    override fun getResLayoutId(): Int {
        return R.layout.fragment_network_list
    }


    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupViewModel()
        setupRecyclerView()
        setupAddGroupFab()
    }


    private fun setupRecyclerView(){
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        networkListRecyclerViewAdapter = NetworkListRecyclerViewAdapter(view!!.context)
        networkListRecyclerViewAdapter.itemClickListener = onNetworkItemClickedListener
        rv_network_list.layoutManager = linearLayoutManager
        rv_network_list.setHasFixedSize(true)
        rv_network_list.adapter = networkListRecyclerViewAdapter

    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        networkListViewModel = ViewModelProvider(this, viewModelFactory).get(NetworkListViewModel::class.java)
        networkListViewModel.getNetworkList().observe(this,networkListObserver)
    }

    private fun setupAddGroupFab() {
        Timber.d("setupAddGroupFab")
        fab_add_network.setOnClickListener {
            Timber.d("onAddGroupClick")
            val addNetworkDialog = AddNetworkDialog()
            addNetworkDialog.show(fragmentManager!!, "AddNetworkDialog")
            addNetworkDialog.setAddNetworkClickListener(onAddNetworkClickListener)
        }
    }

    private val onNetworkItemClickedListener = object : BaseRecyclerViewAdapter.ItemClickListener<Subnet> {
        override fun onClick(position: Int, item: Subnet) {
            Timber.d("onNetworkItemClickedListener: clicked")
            networkListViewModel.setCurrentNetwork(item)

            val mainActivity = activity as MainActivity
            mainActivity.replaceFragment(NetworkFragment(),NetworkFragment.TAG,R.id.container_main)
        }
    }

    private val networkListObserver = Observer<Set<Subnet>> {
       activity?.runOnUiThread {
           networkListRecyclerViewAdapter.setDataList(it.toMutableList())
       }
    }

    private val onAddNetworkClickListener = object : AddNetworkClickListener {
        override fun onClicked() {
            networkListViewModel.getNetworkList()
        }
    }
}