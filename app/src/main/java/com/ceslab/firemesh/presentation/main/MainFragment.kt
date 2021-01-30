package com.ceslab.firemesh.presentation.main

import android.view.View
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.network.NetworkFragment
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
        replaceFragment(NetworkFragment(), NetworkFragment.TAG, R.id.container_nav)
        val navigation = view.findViewById<BottomNavigationView>(R.id.bottom_nav)
        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_item_network -> {
                    replaceFragment(NetworkFragment(), NetworkFragment.TAG, R.id.container_nav)
                    true

                }
                R.id.nav_item_scan -> {
                    replaceFragment(ScanFragment(), ScanFragment.TAG, R.id.container_nav)
                    true
                }
                else -> false
            }
        }
    }
}