package com.ceslab.firemesh.presentation.scan

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.ConnectableDeviceDescription
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.scan.dialog.ProvisionBottomDialog
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_scan.*
import timber.log.Timber

class ScanFragment : BaseFragment() {
    companion object {
        const val TAG = "ScanFragment"
    }


    private lateinit var scannerRecyclerViewAdapter: ScanRecyclerViewAdapter
    private lateinit var scanViewModel: ScanViewModel
    private  var isViewCreated = false

    override fun getResLayoutId(): Int {
        return R.layout.fragment_scan
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop")
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
        if(!isVisibleToUser && isViewCreated) {
            scanViewModel.stopScan()
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
        scanViewModel = ViewModelProvider(this, viewModelFactory).get(ScanViewModel::class.java)
        scanViewModel.isLeScanStarted().observe(this, isLeScanStartedObserver)
        scanViewModel.scannedDeviceResult.observe(this, scanUnprovisionedDeviceObserver)
    }

    private fun setupViews() {
        Timber.d("setupViews")
        btn_scanning.setOnClickListener(onScanButtonClickListener)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        scannerRecyclerViewAdapter = ScanRecyclerViewAdapter(view!!.context)
        scannerRecyclerViewAdapter.itemClickListener = onProvisionButtonClickedListener
        rv_scan.layoutManager = linearLayoutManager
        rv_scan.setHasFixedSize(true)
        rv_scan.adapter = scannerRecyclerViewAdapter
    }

    private val onScanButtonClickListener = View.OnClickListener {
        Timber.d("onScanButtonClickListener: clicked")
        scanViewModel.scanUnprovisionedDevice()
    }

    private val isLeScanStartedObserver = Observer<Boolean> {
        activity?.runOnUiThread {
            if (it) {
                btn_scanning.text = "Stop Scanning"
                tv_scanning_message.text = "Looking for nearby devices..."
                btn_scanning.setBackgroundColor(Color.parseColor("#D91E2A"))
            } else {
                btn_scanning.text = "Start Scanning"
                tv_scanning_message.text = "Press start to scan device"
                btn_scanning.setBackgroundColor(Color.parseColor("#0288D1"))
            }
        }
    }

    private val scanUnprovisionedDeviceObserver = Observer<ConnectableDeviceDescription> {
        activity?.runOnUiThread {
            looking_for_devices_background.visibility = View.INVISIBLE
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
                bundle.putSerializable(ProvisionBottomDialog.DEVICE_DESCRIPTION_KEY,item)
                provisionBottomDialog.arguments = bundle
                provisionBottomDialog.show(fragmentManager!!, "ProvisionBottomDialog")
            }
        }
}