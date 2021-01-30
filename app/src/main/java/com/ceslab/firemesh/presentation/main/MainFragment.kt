package com.ceslab.firemesh.presentation.main

import android.view.View
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.network_list.NetworkListFragment
import com.ceslab.firemesh.presentation.scan.ScanFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import timber.log.Timber

class MainFragment : BaseFragment() {
    companion object {
        const val TAG = "MainFragment"
    }


    override fun getResLayoutId(): Int {
        return R.layout.fragment_main
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupBottomNavigationView(view)
    }

    private fun setupBottomNavigationView(view: View) {
        Timber.d("setupBottomNavigationView")
        replaceFragment(NetworkListFragment(), NetworkListFragment.TAG, R.id.container_nav_main)
        val navigation = view.findViewById<BottomNavigationView>(R.id.bottom_nav_main)
        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_item_networks -> {
                    replaceFragment(NetworkListFragment(), NetworkListFragment.TAG, R.id.container_nav_main)
                    true

                }
                R.id.nav_item_scan -> {
                    replaceFragment(ScanFragment(), ScanFragment.TAG, R.id.container_nav_main)
                    true
                }
                else -> false
            }
        }
    }
}