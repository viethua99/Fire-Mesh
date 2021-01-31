package com.ceslab.firemesh.presentation.node

import android.view.View
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.group_list.GroupListRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_node.*
import timber.log.Timber

class NodeFragment : BaseFragment(){
    companion object {
        const val TAG = "NodeFragment"
    }

    private lateinit var nodePagerAdapter: NodePagerAdapter
    override fun getResLayoutId(): Int {
       return R.layout.fragment_node
    }



    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupDeviceViewPager()
    }

    private fun setupDeviceViewPager() {
        Timber.d("setupDeviceViewPager")
        nodePagerAdapter = NodePagerAdapter(childFragmentManager)
        view_pager_node.adapter = nodePagerAdapter
        tab_layout_node.setupWithViewPager(view_pager_node)
    }
}