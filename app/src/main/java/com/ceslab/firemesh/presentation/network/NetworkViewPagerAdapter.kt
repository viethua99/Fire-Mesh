package com.ceslab.firemesh.presentation.network

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.ceslab.firemesh.presentation.group_list.GroupListFragment
import com.ceslab.firemesh.presentation.node_list.NodeListFragment

class NetworkViewPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {
    companion object {
        const val GROUP_LIST_PAGE = 0
        const val NODE_LIST_PAGE = 1
        const val MAX_PAGES = 2
    }

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> GroupListFragment()
            else -> NodeListFragment()
        }
    }

    override fun getCount(): Int {
        return MAX_PAGES
    }
}