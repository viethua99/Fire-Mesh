package com.ceslab.firemesh.presentation.network

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
import kotlinx.android.synthetic.main.fragment_network.*
import timber.log.Timber

class NetworkFragment : BaseFragment(){
    companion object {
        const val TAG = "NetworkFragment"
    }

    private lateinit var networkViewModel: NetworkViewModel

    override fun getResLayoutId(): Int {
        return R.layout.fragment_network
    }

    override fun onMyViewCreated(view: View) {
       Timber.d("onMyViewCreated")
        setupViewModel()
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupBottomNavigationView()
        setupViewPager()
        connectToNetwork()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        disconnectFromNetwork()
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        networkViewModel = ViewModelProvider(this, viewModelFactory).get(NetworkViewModel::class.java)
        networkViewModel.getMeshStatus().observe(this,meshStatusObserver)
        networkViewModel.getConnectionMessage().observe(this,connectionMessageObserver)
        networkViewModel.getErrorMessage().observe(this,errorMessageObserver)

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

    private fun connectToNetwork(){
        Timber.d("connectToNetwork")
        networkViewModel.connectToNetwork()
    }

    private fun disconnectFromNetwork(){
        Timber.d("disconnectFromNode")
        networkViewModel.disconnectFromNetwork()
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

        }
    }
    private val errorMessageObserver = Observer<ErrorType> {
        activity?.runOnUiThread {

        }
    }

}