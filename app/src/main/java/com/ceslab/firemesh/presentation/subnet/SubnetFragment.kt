package com.ceslab.firemesh.presentation.subnet

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.listener.ConnectionMessageListener
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.util.AppUtil
import com.siliconlab.bluetoothmesh.adk.ErrorType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_subnet.*
import timber.log.Timber

class SubnetFragment(private val subnetName: String) : BaseFragment() {
    companion object {
        const val TAG = "SubnetFragment"
    }

    private lateinit var subnetViewModel: SubnetViewModel

    override fun getResLayoutId(): Int {
        return R.layout.fragment_subnet
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setHasOptionsMenu(true)
        setupViewModel()
        setupViews()
        connectToSubnet()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        disconnectFromSubnet()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        setupToolbarTitle(subnetName)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun updateGroupListSize() {
        tv_count.text = "${subnetViewModel.getGroupListSize()} Groups"
    }

    fun updateNodeListSize() {
        tv_count.text = "${subnetViewModel.getNodeListSize()} Nodes"
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        subnetViewModel = ViewModelProvider(this, viewModelFactory).get(SubnetViewModel::class.java)
        subnetViewModel.apply {
            getMeshStatus().observe(this@SubnetFragment, meshStatusObserver)
            getConnectionMessage().observe(this@SubnetFragment, connectionMessageObserver)
            getErrorMessage().observe(this@SubnetFragment, errorMessageObserver)
        }

    }

    private fun setupViews() {
        setupBottomNavigationView()
        setupViewPager()
        updateGroupListSize()
    }


    private fun setupViewPager() {
        Timber.d("setupViewPager")
        val subnetViewPagerAdapter = SubnetViewPagerAdapter(childFragmentManager)
        subnet_view_pager.apply {
            adapter = subnetViewPagerAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    when (position) {
                        SubnetViewPagerAdapter.GROUP_LIST_PAGE -> {
                            updateGroupListSize()
                            bottom_nav_subnet.menu.findItem(R.id.nav_item_groups).isChecked = true
                        }
                        SubnetViewPagerAdapter.NODE_LIST_PAGE -> {
                            updateNodeListSize()
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
                    subnet_view_pager.currentItem = SubnetViewPagerAdapter.NODE_LIST_PAGE
                    true
                }
                else -> false
            }
        }
    }

    private fun connectToSubnet() {
        Timber.d("connectToSubnet")
        subnetViewModel.connectToSubnet()
    }

    private fun disconnectFromSubnet() {
        Timber.d("disconnectFromSubnet")
        subnetViewModel.disconnectFromSubnet()
    }



    private val meshStatusObserver = Observer<MeshStatus> { meshStatus ->
        Timber.d("meshStatusObserver = $meshStatus")
        activity?.runOnUiThread {
            tv_subnet_connection.apply {
                when (meshStatus) {
                    MeshStatus.MESH_CONNECTING -> {
                        text = "Connecting"
                        setTextColor(Color.parseColor("#ffad33"))
                        btn_subnet_connect.text = "Disconnect"
                        progress_bar_connection.visibility = View.VISIBLE
                    }
                    MeshStatus.MESH_CONNECTED -> {
                        text = "Connected"
                        setTextColor(Color.parseColor("#70bf73"))
                        btn_subnet_connect.text = "Disconnect"
                        progress_bar_connection.visibility = View.GONE
                    }
                    MeshStatus.MESH_DISCONNECTED -> {
                        text = "Disconnected"
                        setTextColor(Color.parseColor("#ff7373"))
                        btn_subnet_connect.text = "Connect"
                        progress_bar_connection.visibility = View.GONE
                    }
                }
            }

          btn_subnet_connect.setOnClickListener {
                subnetViewModel.changeMeshStatus(meshStatus)
            }
        }
    }
    private val connectionMessageObserver = Observer<ConnectionMessageListener.MessageType> {
        activity?.runOnUiThread {
            when (it) {
                ConnectionMessageListener.MessageType.NO_NODE_IN_SUBNET -> showWarningDialog("No node in this subnet")
                ConnectionMessageListener.MessageType.GATT_NOT_CONNECTED -> showWarningDialog("Bluetooth Gatt is not connected")
                ConnectionMessageListener.MessageType.GATT_PROXY_DISCONNECTED -> showWarningDialog("Remote proxy disconnected")
                ConnectionMessageListener.MessageType.GATT_ERROR_DISCOVERING_SERVICES -> showWarningDialog(
                    "Error discovering services"
                )
                ConnectionMessageListener.MessageType.PROXY_SERVICE_NOT_FOUND -> showWarningDialog("Mesh GATT Service is not found")
                ConnectionMessageListener.MessageType.PROXY_CHARACTERISTIC_NOT_FOUND -> showWarningDialog(
                    "Mesh GATT Characteristic is not found"
                )
                ConnectionMessageListener.MessageType.PROXY_DESCRIPTOR_NOT_FOUND -> showWarningDialog(
                    "Mesh GATT Descriptor is not found"
                )
            }
        }
    }
    private val errorMessageObserver = Observer<ErrorType> {
        activity?.runOnUiThread {
            showFailedDialog(AppUtil.convertErrorMessage(activity!!, it))
        }
    }

}