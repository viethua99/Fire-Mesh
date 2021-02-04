package com.ceslab.firemesh.presentation.main.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.ceslab.firemesh.presentation.network_list.NetworkListFragment
import com.ceslab.firemesh.presentation.scan.ScanFragment

class MainViewPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {
    companion object {
        const val NETWORK_LIST_PAGE = 0
        const val SCAN_PAGE = 1
        const val MAX_PAGES = 2
    }

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> NetworkListFragment()
            else -> ScanFragment()
        }
    }

    override fun getCount(): Int {
        return MAX_PAGES
    }
}