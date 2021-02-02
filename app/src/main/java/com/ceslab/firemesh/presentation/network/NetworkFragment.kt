package com.ceslab.firemesh.presentation.network

import android.view.View
import androidx.viewpager.widget.ViewPager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.group_list.GroupListFragment
import com.ceslab.firemesh.presentation.node_list.NodeListFragment
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.main.fragment.MainViewPagerAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.fragment_network.*
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
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupBottomNavigationView()
        setupViewPager()
    }


    private fun setupViewPager() {
        Timber.d("setupViewPager")
        (activity as MainActivity).supportActionBar?.title = getString(R.string.nav_item_groups)
        val networkViewPagerAdapter = NetworkViewPagerAdapter(childFragmentManager)
        network_view_pager.adapter = networkViewPagerAdapter
        network_view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                when (position) {
                    NetworkViewPagerAdapter.GROUP_LIST_PAGE ->{
                        bottom_nav_network.menu.findItem(R.id.nav_item_groups).isChecked = true
                        (activity as MainActivity).supportActionBar?.title = getString(R.string.nav_item_groups)
                    }
                    NetworkViewPagerAdapter.NODE_LIST_PAGE ->  {
                        bottom_nav_network.menu.findItem(R.id.nav_item_nodes).isChecked = true
                        (activity as MainActivity).supportActionBar?.title = getString(R.string.nav_item_nodes)
                    }
                }
            }
        })
    }

    private fun setupBottomNavigationView() {
        Timber.d("setupBottomNavigationView")
        bottom_nav_network.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_item_groups -> {
                    network_view_pager.currentItem = NetworkViewPagerAdapter.GROUP_LIST_PAGE
                    true

                }
                R.id.nav_item_nodes -> {
                    network_view_pager.currentItem =NetworkViewPagerAdapter.NODE_LIST_PAGE
                    true
                }
                else -> false
            }
        }
    }
}