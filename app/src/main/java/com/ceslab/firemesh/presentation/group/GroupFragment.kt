package com.ceslab.firemesh.presentation.group

import android.graphics.Color
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
import com.siliconlab.bluetoothmesh.adk.ErrorType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_group.*
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
        setupViewModel()
        setupRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        groupViewModel.removeListeners()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as MainActivity).supportActionBar?.title = groupName
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




    private fun showConnectingAnimation() {
        Timber.d("showConnectingAnimation")
        activity?.runOnUiThread {
            val connectingGradientAnimation = AnimationUtils.loadAnimation(activity, R.anim.connection_translate_right)
            connecting_anim_gradient_right_container.visibility = View.VISIBLE
            connecting_anim_gradient_right_container.startAnimation(connectingGradientAnimation)
        }
    }

    private fun hideConnectingAnimation() {
        Timber.d("hideConnectingAnimation")
        activity?.runOnUiThread {
            connecting_anim_gradient_right_container.clearAnimation()
            connecting_anim_gradient_right_container.visibility = View.GONE
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
                        setBackgroundColor(Color.parseColor("#FF9800"))
                        showConnectingAnimation()
                    }
                    MeshStatus.MESH_CONNECTED -> {
                        text = "Connected"
                        setBackgroundColor(Color.parseColor("#4CAF50"))
                        hideConnectingAnimation()
                    }
                    MeshStatus.MESH_DISCONNECTED -> {
                        text = "Disconnected"
                        setBackgroundColor(Color.parseColor("#F44336"))
                        hideConnectingAnimation()
                    }
                }

                setOnClickListener {
                    groupViewModel.changeMeshStatus(meshStatus)
                }
            }

        }
    }

    private val onNodeItemClickedListener = object : BaseRecyclerViewAdapter.ItemClickListener<MeshNode> {
        override fun onClick(position: Int, item: MeshNode) {}
        override fun onLongClick(position: Int, item: MeshNode) {}
    }

    private val connectionMessageObserver = Observer<ConnectionMessageListener.MessageType> {
        activity?.runOnUiThread {
            showWarningDialog(it.name)
        }
    }
    private val errorMessageObserver = Observer<ErrorType> {
        activity?.runOnUiThread {
            showFailedDialog(it.type.name)
        }
    }

}