package com.ceslab.firemesh.presentation.provision_list

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
import com.ceslab.firemesh.presentation.provision_list.dialog.ProvisionBottomDialog
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_provision_list.*
import timber.log.Timber

class ProvisionListFragment : BaseFragment() {
    companion object {
        const val TAG = "ProvisionListFragment"
    }


    private lateinit var scannerRecyclerViewAdapter: ProvisionRecyclerViewAdapter
    private lateinit var provisionViewModel: ProvisionViewModel
    private  var isViewCreated = false

    override fun getResLayoutId(): Int {
        return R.layout.fragment_provision_list
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
        provisionViewModel = ViewModelProvider(this, viewModelFactory).get(ProvisionViewModel::class.java)
        provisionViewModel.isLeScanStarted().observe(this, isLeScanStartedObserver)
        provisionViewModel.scannedDeviceResult.observe(this, scanUnprovisionedDeviceObserver)
    }

    private fun setupViews() {
        Timber.d("setupViews")
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

    private val onScanButtonClickListener = View.OnClickListener {
        Timber.d("onScanButtonClickListener: clicked")
        provisionViewModel.scanUnprovisionedDevice()
    }

    private val isLeScanStartedObserver = Observer<Boolean> {
        activity?.runOnUiThread {
            if (it) {
                btn_scanning.text = getString(R.string.fragment_provision_list_stop_scanning)
                tv_scanning_message.text = getString(R.string.fragment_provision_list_looking_for_nearby_devices)
                btn_scanning.setBackgroundColor(Color.parseColor("#D91E2A"))
            } else {
                btn_scanning.text =getString(R.string.fragment_provision_list_start_scanning)
                tv_scanning_message.text = getString(R.string.fragment_provision_list_press_start_message)
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

            override fun onLongClick(position: Int, item: ConnectableDeviceDescription) {}
        }
}