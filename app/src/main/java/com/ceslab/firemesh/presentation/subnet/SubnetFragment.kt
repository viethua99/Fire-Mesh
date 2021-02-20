package com.ceslab.firemesh.presentation.subnet

import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.listener.ConnectionMessageListener
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.siliconlab.bluetoothmesh.adk.ErrorType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_subnet.*
import timber.log.Timber

class SubnetFragment(private val subnetName:String) : BaseFragment(){
    companion object {
        const val TAG = "SubnetFragment"
    }

    private lateinit var subnetViewModel: SubnetViewModel

    override fun getResLayoutId(): Int {
        return R.layout.fragment_subnet
    }

    override fun onMyViewCreated(view: View) {
       Timber.d("onMyViewCreated")
        setupViewModel()
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupBottomNavigationView()
        setupViewPager()
        connectToSubnet()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        disconnectFromSubnet()
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        subnetViewModel = ViewModelProvider(this, viewModelFactory).get(SubnetViewModel::class.java)
        subnetViewModel.apply {
            getMeshStatus().observe(this@SubnetFragment,meshStatusObserver)
            getConnectionMessage().observe(this@SubnetFragment,connectionMessageObserver)
            getErrorMessage().observe(this@SubnetFragment,errorMessageObserver)
        }

    }



    private fun setupViewPager() {
        Timber.d("setupViewPager")
        (activity as MainActivity).supportActionBar?.title = subnetName
        val subnetViewPagerAdapter = SubnetViewPagerAdapter(childFragmentManager)
        subnet_view_pager.apply {
            adapter = subnetViewPagerAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    when (position) {
                        SubnetViewPagerAdapter.GROUP_LIST_PAGE ->{
                            bottom_nav_subnet.menu.findItem(R.id.nav_item_groups).isChecked = true
                        }
                        SubnetViewPagerAdapter.NODE_LIST_PAGE ->  {
                            bottom_nav_subnet.menu.findItem(R.id.nav_item_nodes).isChecked = true
                        }
                    }
                }
            })
        }
    }

    private fun setupBottomNavigationView() {
        Timber.d("setupBottomNavigationView")
        bottom_nav_subnet.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_item_groups -> {
                    subnet_view_pager.currentItem = SubnetViewPagerAdapter.GROUP_LIST_PAGE
                    true

                }
                R.id.nav_item_nodes -> {
                    subnet_view_pager.currentItem =SubnetViewPagerAdapter.NODE_LIST_PAGE
                    true
                }
                else -> false
            }
        }
    }

    private fun connectToSubnet(){
        Timber.d("connectToSubnet")
        subnetViewModel.connectToSubnet()
    }

    private fun disconnectFromSubnet(){
        Timber.d("disconnectFromSubnet")
        subnetViewModel.disconnectFromSubnet()
    }

    private val meshStatusObserver = Observer<MeshStatus> {
        activity?.runOnUiThread {
            when(it){
                MeshStatus.MESH_CONNECTING -> showProgressDialog("Connecting")
                MeshStatus.MESH_CONNECTED -> hideDialog()
                MeshStatus.MESH_DISCONNECTED -> hideDialog()
            }
        }
    }
    private val connectionMessageObserver = Observer<ConnectionMessageListener.MessageType> {
        activity?.runOnUiThread {
            showWarningDialog(it.name)
        }
    }
    private val errorMessageObserver = Observer<ErrorType> {
        activity?.runOnUiThread {
            showFailedDialog(it.type.name)

        }
    }

}