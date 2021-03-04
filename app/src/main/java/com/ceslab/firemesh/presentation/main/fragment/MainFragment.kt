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
        private const val WRITE_EXTERNAL_STORAGE_REQUEST_PERMISSION = 300

    }


    override fun getResLayoutId(): Int {
        return R.layout.fragment_main
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        setHasOptionsMenu(true)
        setupBottomNavigationView()
        setupViewPager()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main,menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.item_ota -> {
                if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        WRITE_EXTERNAL_STORAGE_REQUEST_PERMISSION
                    )
                } else {
                    OTAListActivity.startOTAListActivity(activity as AppCompatActivity)
                }
            }
        }
        return true
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