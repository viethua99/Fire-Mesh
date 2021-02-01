package com.ceslab.firemesh.presentation.node

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.dialogs.OTADialogConfig
import kotlinx.android.synthetic.main.fragment_node.*
import timber.log.Timber

class NodeFragment : BaseFragment() {
    companion object {
        const val TAG = "NodeFragment"
    }

    private lateinit var nodePagerAdapter: NodePagerAdapter

    override fun getResLayoutId(): Int {
        return R.layout.fragment_node
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) //able to use options menu in fragment
    }


    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupDeviceViewPager()
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
            val otaDialog = OTADialogConfig()
            otaDialog.show(fragmentManager!!,"OtaDialog")
            return true
        }
        return false
    }

    private fun setupDeviceViewPager() {
        Timber.d("setupDeviceViewPager")
        nodePagerAdapter = NodePagerAdapter(childFragmentManager)
        view_pager_node.adapter = nodePagerAdapter
        tab_layout_node.setupWithViewPager(view_pager_node)
    }
}