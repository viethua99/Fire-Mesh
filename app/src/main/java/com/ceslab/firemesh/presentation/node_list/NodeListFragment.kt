package com.ceslab.firemesh.presentation.node_list

import android.os.Build
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.node.NodeFragment
import com.ceslab.firemesh.presentation.node_list.dialog.EditNodeCallback
import com.ceslab.firemesh.presentation.node_list.dialog.EditNodeDialog
import com.ceslab.firemesh.presentation.subnet.SubnetFragment
import com.ceslab.firemesh.service.FireMeshService
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_node_list.*
import timber.log.Timber

class NodeListFragment : BaseFragment(){
    companion object {
        const val TAG = "NodeListFragment"
    }

    private lateinit var nodeListRecyclerViewAdapter: NodeListRecyclerViewAdapter
    private lateinit var nodeListViewModel: NodeListViewModel


    override fun getResLayoutId(): Int {
       return R.layout.fragment_node_list
    }

    override fun onMyViewCreated(view: View) {
       Timber.d("onMyViewCreated")
        setHasOptionsMenu(true)
        setupViewModel()
        setupRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mainActivity = activity as MainActivity
            if(!mainActivity.isServiceRunning(FireMeshService::class.java)){
                nodeListViewModel.stopScan()
            }
        }
        nodeListViewModel.removeListener()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_node_list,menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.item_refresh -> {
              nodeListViewModel.refreshNodeListStatus()
            }
        }
        return true
    }


    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        nodeListViewModel = ViewModelProvider(this, viewModelFactory).get(NodeListViewModel::class.java)
        nodeListViewModel.setListeners()
        nodeListViewModel.getMeshNodeList().observe(this,meshNodeListObserver)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mainActivity = activity as MainActivity
            if(!mainActivity.isServiceRunning(FireMeshService::class.java)){
                nodeListViewModel.startScan()
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

    private val onNodeItemClickedListener = object : BaseRecyclerViewAdapter.ItemClickListener<MeshNode> {
        override fun onClick(position: Int, item: MeshNode) {
            Timber.d("onNodeItemClickedListener: clicked")
            ViewCompat.postOnAnimationDelayed(view!!, // Delay to show ripple effect
                Runnable {
                    nodeListViewModel.setDeviceToConfigure(item)
                    val mainActivity = activity as MainActivity
                    mainActivity.addFragment(NodeFragment(item.node.name), NodeFragment.TAG,R.id.container_main)
                }
                ,50)
        }

        override fun onLongClick(position: Int, item: MeshNode) {
            Timber.d("onNodeItemClickedListener: longClicked")
            val editNodeDialog = EditNodeDialog(item)
            editNodeDialog.show(fragmentManager!!, "DeleteNodeDialog")
            editNodeDialog.setDeleteNodeCallback(onEditNodeCallback)
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

    private val onEditNodeCallback = object : EditNodeCallback {
        override fun onChanged() {
            Timber.d("onChanged")
            nodeListViewModel.getMeshNodeList()
            val subnetFragment = parentFragment as SubnetFragment
            subnetFragment.updateNodeListSize()
        }
    }
}