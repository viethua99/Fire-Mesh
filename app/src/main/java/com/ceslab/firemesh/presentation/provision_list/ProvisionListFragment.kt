package com.ceslab.firemesh.presentation.provision_list

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.ConnectableDeviceDescription
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.provision_list.dialog.ProvisionBottomDialog
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_provision_list.*
import kotlinx.android.synthetic.main.fragment_provision_list.btn_scanning
import kotlinx.android.synthetic.main.fragment_provision_list.tv_scanning_message
import timber.log.Timber

class ProvisionListFragment : BaseFragment() {
    companion object {
        const val TAG = "ProvisionListFragment"
    }

    private lateinit var scannerRecyclerViewAdapter: ProvisionRecyclerViewAdapter
    private lateinit var provisionViewModel: ProvisionViewModel
    private var isViewCreated = false

    override fun getResLayoutId(): Int {
        return R.layout.fragment_provision_list
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")


    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop")
        provisionViewModel.stopScan()
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")

    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (!isVisibleToUser && isViewCreated) {
            provisionViewModel.stopScan()
        }
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupViewModel()
        setupViews()
        isViewCreated = true
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        provisionViewModel =
            ViewModelProvider(this, viewModelFactory).get(ProvisionViewModel::class.java)
        provisionViewModel.isLeScanStarted().observe(this, isLeScanStartedObserver)
        provisionViewModel.scannedDeviceResult.observe(this, scanUnprovisionedDeviceObserver)
    }

    private fun setupViews() {
        Timber.d("setupViews")
        pull_to_refresh.setOnRefreshListener {
            scannerRecyclerViewAdapter.clear()
            pull_to_refresh.isRefreshing = false

        }
        btn_scanning.setOnClickListener(onScanButtonClickListener)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        scannerRecyclerViewAdapter = ProvisionRecyclerViewAdapter(view!!.context)
        scannerRecyclerViewAdapter.itemClickListener = onProvisionButtonClickedListener
        rv_provision.apply {
            layoutManager = linearLayoutManager
            setHasFixedSize(true)
            adapter = scannerRecyclerViewAdapter
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        return locationManager?.let {
            it.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || it.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )
        } ?: false
    }

    private val onScanButtonClickListener = View.OnClickListener {
        Timber.d("onScanButtonClickListener: clicked")
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled || !isLocationEnabled()) {
            showToastMessage("Please enable bluetooth and location")
        } else {
            provisionViewModel.scanUnprovisionedDevice()
        }
    }

    private val isLeScanStartedObserver = Observer<Boolean> {
        activity?.runOnUiThread {
            if (it) {
                btn_scanning.text = getString(R.string.fragment_ota_list_stop_scanning)
                btn_scanning.setBackgroundColor(Color.parseColor("#ff5050"))
                if(scannerRecyclerViewAdapter.dataList.isEmpty()){
                    tv_scanning_message.visibility = View.GONE
                    bg_ripple.visibility = View.VISIBLE
                    bg_ripple.startRippleAnimation()
                }
            } else {
                btn_scanning.text = getString(R.string.fragment_provision_list_start_scanning)
                tv_scanning_message.visibility = View.VISIBLE
                tv_scanning_message.text =
                    getString(R.string.fragment_provision_list_press_start_message)
                btn_scanning.setBackgroundColor(Color.parseColor("#007bff"))
                bg_ripple.stopRippleAnimation()

            }
        }
    }

    private val scanUnprovisionedDeviceObserver = Observer<ConnectableDeviceDescription> {
        activity?.runOnUiThread {
            bg_ripple.stopRippleAnimation()
            bg_ripple.visibility = View.GONE
            scannerRecyclerViewAdapter.replaceDeviceByIndex(it)

        }
    }

    private val onProvisionButtonClickedListener =
        object : BaseRecyclerViewAdapter.ItemClickListener<ConnectableDeviceDescription> {
            override fun onClick(position: Int, item: ConnectableDeviceDescription) {
                Timber.d("onProvisionButtonClickedListener: clicked")
                val provisionBottomDialog =
                    ProvisionBottomDialog()
                val bundle = Bundle()
                bundle.putSerializable(ProvisionBottomDialog.DEVICE_DESCRIPTION_KEY, item)
                provisionBottomDialog.arguments = bundle
                provisionBottomDialog.show(fragmentManager!!, "ProvisionBottomDialog")
            }

            override fun onLongClick(position: Int, item: ConnectableDeviceDescription) {}
        }
}