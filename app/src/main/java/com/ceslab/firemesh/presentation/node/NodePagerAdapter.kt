package com.ceslab.firemesh.presentation.node

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.ceslab.firemesh.presentation.node.node_config.NodeConfigFragment
import com.ceslab.firemesh.presentation.node.node_info.NodeInfoFragment

class NodePagerAdapter(fragmentManager: FragmentManager?) :
    FragmentPagerAdapter(fragmentManager!!) {
    private val MAX_PAGES = 2
    private val tabTitles = listOf("Config", "Info")
    override fun getItem(position: Int): Fragment {
        when (position) {
            0 -> return NodeConfigFragment()
            1 -> return NodeInfoFragment()
        }
        return Fragment()
    }

    override fun getCount(): Int {
        return MAX_PAGES
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return tabTitles[position]
    }
}