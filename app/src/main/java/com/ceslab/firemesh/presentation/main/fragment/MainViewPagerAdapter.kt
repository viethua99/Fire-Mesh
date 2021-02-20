package com.ceslab.firemesh.presentation.main.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.ceslab.firemesh.presentation.subnet_list.SubnetListFragment
import com.ceslab.firemesh.presentation.ota_list.OTAListFragment
import com.ceslab.firemesh.presentation.provision_list.ProvisionListFragment

class MainViewPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {
    companion object {
        const val SUBNET_LIST_PAGE = 0
        const val PROVISION_LIST = 1
        const val OTA_LIST_PAGE = 2
        const val MAX_PAGES = 3
    }

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> SubnetListFragment()
            1 -> ProvisionListFragment()
            else -> OTAListFragment()
        }
    }

    override fun getCount(): Int {
        return MAX_PAGES
    }
}