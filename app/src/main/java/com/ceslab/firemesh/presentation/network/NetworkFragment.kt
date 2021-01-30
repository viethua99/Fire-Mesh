package com.ceslab.firemesh.presentation.network

import android.view.View
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.group_list.GroupListFragment
import com.ceslab.firemesh.presentation.node_list.NodeListFragment
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import timber.log.Timber

class NetworkFragment : BaseFragment(){
    companion object {
        const val TAG = "NetworkFragment"
    }
    override fun getResLayoutId(): Int {
        return R.layout.fragment_network
    }

    override fun onMyViewCreated(view: View) {
       Timber.d("onMyViewCreated")
        setupBottomNavigationView(view)
    }

    private fun setupBottomNavigationView(view: View) {
        Timber.d("setupBottomNavigationView")
        replaceFragment(GroupListFragment(), GroupListFragment.TAG, R.id.container_nav_network)
        val navigation = view.findViewById<BottomNavigationView>(R.id.bottom_nav_network)
        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_item_groups -> {
                    replaceFragment(GroupListFragment(), GroupListFragment.TAG, R.id.container_nav_network)
                    true

                }
                R.id.nav_item_nodes -> {
                    replaceFragment(NodeListFragment(), NodeListFragment.TAG, R.id.container_nav_network)
                    true
                }
                else -> false
            }
        }
    }
}