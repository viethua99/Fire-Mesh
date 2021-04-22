package com.ceslab.firemesh.presentation.main.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.ceslab.firemesh.presentation.subnet_list.SubnetListFragment
import com.ceslab.firemesh.presentation.provision_list.ProvisionListFragment

class MainViewPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {
    companion object {
        const val SUBNET_LIST_PAGE = 0
        const val PROVISION_LIST = 1
        const val MAX_PAGES = 2
    }

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> SubnetListFragment()
            else -> ProvisionListFragment()
        }
    }

    override fun getCount(): Int {
        return MAX_PAGES
    }
}