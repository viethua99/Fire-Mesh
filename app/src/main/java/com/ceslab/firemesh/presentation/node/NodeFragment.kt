package com.ceslab.firemesh.presentation.node

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.dialogs.OTADialogConfig
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.scan.dialog.ProvisionBottomDialog
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_node.*
import timber.log.Timber

class NodeFragment : BaseFragment() {
    companion object {
        const val TAG = "NodeFragment"
        const val IS_OTA_INIT_KEY = "IS_OTA_INIT_KEY"
        private const val WRITE_EXTERNAL_STORAGE_REQUEST_PERMISSION = 300
    }

    private lateinit var nodePagerAdapter: NodePagerAdapter
    private lateinit var nodeViewModel: NodeViewModel
    private var isFirstConfig: Boolean = false

    override fun getResLayoutId(): Int {
        return R.layout.fragment_node
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setHasOptionsMenu(true) //able to use options menu in fragment

        arguments?.let {
            if (it.containsKey(ProvisionBottomDialog.IS_FIRST_CONFIG_KEY)) {
                isFirstConfig = it.getBoolean(ProvisionBottomDialog.IS_FIRST_CONFIG_KEY)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        disconnectFromNode()
    }


    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupViewModel()
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        setupDeviceViewPager()
        connectToNode()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Timber.d("onCreateOptionsMenu")
        inflater.inflate(R.menu.menu_ota_node, menu)
        val scanItem = menu.findItem(R.id.item_ota)
        scanItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected: ${item.title}")
        if (item.itemId == R.id.item_ota) {
            if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQUEST_PERMISSION)
            } else {
                val otaDialog = OTADialogConfig()
                val bundle = Bundle()
                bundle.putBoolean(IS_OTA_INIT_KEY,true)
                otaDialog.arguments = bundle
                otaDialog.show(fragmentManager!!,"OtaDialog")
            }

            return true
        }
        return false
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        nodeViewModel = ViewModelProvider(this, viewModelFactory).get(NodeViewModel::class.java)
        nodeViewModel.isFirstConfig = isFirstConfig
        nodeViewModel.getMeshStatus().observe(this,meshStatusObserver)
    }

    private fun setupDeviceViewPager() {
        Timber.d("setupDeviceViewPager")
        nodePagerAdapter = NodePagerAdapter(childFragmentManager)
        view_pager_node.adapter = nodePagerAdapter
        tab_layout_node.setupWithViewPager(view_pager_node)
    }

    private fun connectToNode(){
        Timber.d("connectToNode")
        nodeViewModel.connectToNode()
    }

    private fun disconnectFromNode(){
        Timber.d("disconnectFromNode")
        nodeViewModel.disconnectFromNode()
    }

    private val meshStatusObserver = Observer<MeshStatus> {
        activity?.runOnUiThread {
            when(it) {
                MeshStatus.MESH_CONNECTING -> showProgressDialog("Connecting to node")
                MeshStatus.MESH_CONNECTED -> hideDialog()
                MeshStatus.MESH_DISCONNECTED -> hideDialog()
                MeshStatus.INIT_CONFIGURATION_LOADED ->  hideDialog()
            }
        }
    }
}