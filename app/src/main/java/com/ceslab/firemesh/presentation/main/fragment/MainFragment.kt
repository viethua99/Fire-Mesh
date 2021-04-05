package com.ceslab.firemesh.presentation.main.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.ota_list.OTAListActivity
import kotlinx.android.synthetic.main.fragment_main.*
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
        setHasOptionsMenu(true)
        setupBottomNavigationView()
        setupViewPager()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun setupViewPager() {
        Timber.d("setupViewPager")
        (activity as MainActivity).supportActionBar?.title = getString(R.string.nav_item_subnets)
        val mainViewPagerAdapter = MainViewPagerAdapter(childFragmentManager)
        main_view_pager.adapter = mainViewPagerAdapter
        main_view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                when (position) {
                    MainViewPagerAdapter.SUBNET_LIST_PAGE ->{
                        bottom_nav_main.menu.findItem(R.id.nav_item_networks).isChecked = true
                        (activity as MainActivity).supportActionBar?.title = getString(R.string.nav_item_subnets)
                    }
                    MainViewPagerAdapter.PROVISION_LIST ->  {
                        bottom_nav_main.menu.findItem(R.id.nav_item_provision).isChecked = true
                        (activity as MainActivity).supportActionBar?.title = getString(R.string.nav_item_provision)
                    }
                }
            }
        })
    }

    private fun setupBottomNavigationView() {
        Timber.d("setupBottomNavigationView")
        bottom_nav_main.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_item_networks -> {
                    main_view_pager.currentItem = MainViewPagerAdapter.SUBNET_LIST_PAGE
                    true

                }
                R.id.nav_item_provision -> {
                    main_view_pager.currentItem = MainViewPagerAdapter.PROVISION_LIST
                    true
                }
                else -> false
            }
        }
    }
}