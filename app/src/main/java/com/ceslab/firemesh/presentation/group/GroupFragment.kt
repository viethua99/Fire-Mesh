package com.ceslab.firemesh.presentation.group

import android.graphics.Color
import android.os.Build
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.listener.ConnectionMessageListener
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.node_list.NodeListRecyclerViewAdapter
import com.ceslab.firemesh.service.FireMeshService
import com.ceslab.firemesh.util.AppUtil
import com.siliconlab.bluetoothmesh.adk.ErrorType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_group.*
import kotlinx.android.synthetic.main.fragment_group.progress_bar_connection
import kotlinx.android.synthetic.main.fragment_group.rv_node_list

import timber.log.Timber

/**
 * Created by Viet Hua on 03/11/2021.
 */

class GroupFragment(private val groupName: String) : BaseFragment() {
    companion object {
        const val TAG = "GroupFragment"
    }

    private lateinit var groupViewModel: GroupViewModel
    private lateinit var nodeListRecyclerViewAdapter: NodeListRecyclerViewAdapter


    override fun getResLayoutId(): Int {
        return R.layout.fragment_group
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setHasOptionsMenu(true)
        setupViewModel()
        setupRecyclerView()
        updateNodeListSize()

    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mainActivity = activity as MainActivity
            if(!mainActivity.isServiceRunning(FireMeshService::class.java)){
                groupViewModel.stopScan()
            }
        }
        groupViewModel.removeListeners()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupToolbarTitle(groupName)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_node_list,menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.item_refresh -> {
                groupViewModel.refreshNodeListStatus()
            }
        }
        return true
    }

    fun updateNodeListSize() {
        tv_count.text = "${groupViewModel.getNodeListSize()} Nodes"
    }


    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        groupViewModel = ViewModelProvider(this, viewModelFactory).get(GroupViewModel::class.java)
        groupViewModel.apply {
            setListeners()
            getMeshNodeList().observe(this@GroupFragment,meshNodeListObserver)
            getMeshStatus().observe(this@GroupFragment,meshStatusObserver)
            getConnectionMessage().observe(this@GroupFragment, connectionMessageObserver)
            getErrorMessage().observe(this@GroupFragment, errorMessageObserver)


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mainActivity = activity as MainActivity
                if(!mainActivity.isServiceRunning(FireMeshService::class.java)){
                    groupViewModel.startScan()
                }
            }
        }
    }

    private fun setupRecyclerView(){
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        nodeListRecyclerViewAdapter = NodeListRecyclerViewAdapter(view!!.context)
        nodeListRecyclerViewAdapter.itemClickListener = onNodeItemClickedListener
        rv_node_list.apply {
            layoutManager = linearLayoutManager
            setHasFixedSize(true)
            adapter = nodeListRecyclerViewAdapter
        }
    }



    private val meshNodeListObserver = Observer<Set<MeshNode>> {
        if(it.isNotEmpty()){
            no_node_background.visibility = View.GONE
            nodeListRecyclerViewAdapter.setDataList(it.toMutableList())
        }  else {
            nodeListRecyclerViewAdapter.clear()
            no_node_background.visibility = View.VISIBLE
        }
    }

    private val meshStatusObserver = Observer<MeshStatus> { meshStatus ->
        Timber.d("meshStatusObserver = $meshStatus")
        activity?.runOnUiThread {
            tv_group_connection.apply {
                when (meshStatus) {
                    MeshStatus.MESH_CONNECTING -> {
                        text = "Connecting"
                        setTextColor(Color.parseColor("#ffad33"))
                        btn_group_connect.text = "Disconnect"
                        progress_bar_connection.visibility = View.VISIBLE
                    }
                    MeshStatus.MESH_CONNECTED -> {
                        text = "Connected"
                        setTextColor(Color.parseColor("#70bf73"))
                        btn_group_connect.text = "Disconnect"
                        progress_bar_connection.visibility = View.GONE
                    }
                    MeshStatus.MESH_DISCONNECTED -> {
                        text = "Disconnected"
                        setTextColor(Color.parseColor("#ff7373"))
                        btn_group_connect.text = "Connect"
                        progress_bar_connection.visibility = View.GONE
                    }
                }

            }
            btn_group_connect.setOnClickListener {
                groupViewModel.changeMeshStatus(meshStatus)
            }
        }
    }

    private val onNodeItemClickedListener = object : BaseRecyclerViewAdapter.ItemClickListener<MeshNode> {
        override fun onClick(position: Int, item: MeshNode) {}
        override fun onLongClick(position: Int, item: MeshNode) {}
    }

    private val connectionMessageObserver = Observer<ConnectionMessageListener.MessageType> {
        activity?.runOnUiThread {
            when(it){
                ConnectionMessageListener.MessageType.NO_NODE_IN_SUBNET -> showWarningDialog("No node in this subnet")
                ConnectionMessageListener.MessageType.GATT_NOT_CONNECTED -> showWarningDialog("Bluetooth Gatt is not connected")
                ConnectionMessageListener.MessageType.GATT_PROXY_DISCONNECTED -> showWarningDialog("Remote proxy disconnected")
                ConnectionMessageListener.MessageType.GATT_ERROR_DISCOVERING_SERVICES -> showWarningDialog("Error discovering services")
                ConnectionMessageListener.MessageType.PROXY_SERVICE_NOT_FOUND -> showWarningDialog("Mesh GATT Service is not found")
                ConnectionMessageListener.MessageType.PROXY_CHARACTERISTIC_NOT_FOUND -> showWarningDialog("Mesh GATT Characteristic is not found")
                ConnectionMessageListener.MessageType.PROXY_DESCRIPTOR_NOT_FOUND -> showWarningDialog("Mesh GATT Descriptor is not found")
            }
        }
    }
    private val errorMessageObserver = Observer<ErrorType> {
        activity?.runOnUiThread {
            showFailedDialog(AppUtil.convertErrorMessage(activity!!,it))
        }
    }

}