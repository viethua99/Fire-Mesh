package com.ceslab.firemesh.presentation.ota_list

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.ConnectableDeviceDescription
import com.ceslab.firemesh.meshmodule.ota.OTADevice
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.node.NodeFragment
import com.ceslab.firemesh.presentation.node_list.OTAListRecyclerViewAdapter
import com.ceslab.firemesh.presentation.ota_list.dialog.ota_config_dialog.OTAConfigDialog
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_ota_list.*
import kotlinx.android.synthetic.main.fragment_ota_list.btn_scanning
import timber.log.Timber

class OTAListFragment : BaseFragment() {
    companion object {
        const val TAG = "OTAFragment"
        private const val WRITE_EXTERNAL_STORAGE_REQUEST_PERMISSION = 300

    }


    private lateinit var otaListRecyclerViewAdapter : OTAListRecyclerViewAdapter
    private lateinit var otaListViewModel: OTAListViewModel
    private  var isViewCreated = false
    override fun getResLayoutId(): Int {
        return R.layout.fragment_ota_list
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if(!isVisibleToUser && isViewCreated) {
            otaListViewModel.stopScan()
        }
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupViewModel()
        setupViews()
    }


    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        otaListViewModel = ViewModelProvider(this, viewModelFactory).get(OTAListViewModel::class.java)
        otaListViewModel.isLeScanStarted().observe(this, isLeScanStartedObserver)
        otaListViewModel.scannedDeviceResult.observe(this, scanOTADeviceObserver)
        otaListViewModel.getConnectStatus().observe(this,connectStatusObserver)
    }

    private fun setupViews() {
        Timber.d("setupViews")
        btn_scanning.setOnClickListener(onScanButtonClickListener)
        setupRecyclerView()
        isViewCreated = true
    }

    private fun setupRecyclerView() {
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        otaListRecyclerViewAdapter = OTAListRecyclerViewAdapter(view!!.context)
        otaListRecyclerViewAdapter.itemClickListener = onOTAButtonClickedListener
        rv_ota_list.layoutManager = linearLayoutManager
        rv_ota_list.setHasFixedSize(true)
        rv_ota_list.adapter = otaListRecyclerViewAdapter
    }



    private val onScanButtonClickListener = View.OnClickListener {
        Timber.d("onScanButtonClickListener: clicked")
        otaListViewModel.scanOTADevice()
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

    private val scanOTADeviceObserver = Observer<OTADevice> {
        Timber.d("scanOTADeviceObserver")
        activity?.runOnUiThread {
            looking_for_devices_background.visibility = View.INVISIBLE
            otaListRecyclerViewAdapter.replaceDeviceByIndex(it)

        }
    }

    private val connectStatusObserver = Observer<String> {
        activity?.runOnUiThread {
            when(it) {
                "GATT_CONNECTED" -> {
                    val otaDialog = OTAConfigDialog()
                   val bundle = Bundle()
                    bundle.putBoolean(NodeFragment.IS_OTA_INIT_KEY,true)
                    otaDialog.arguments = bundle
                    otaDialog.show(fragmentManager!!,"OtaDialog")
                }
                "GATT_DISCONNECTED" -> hideDialog()
                "GATT_FAILED" -> showFailedDialog("Connect Failed")
            }
        }
    }

    private val onOTAButtonClickedListener =
        object : BaseRecyclerViewAdapter.ItemClickListener<OTADevice> {
            override fun onClick(position: Int, item: OTADevice) {
                Timber.d("onOTAButtonClickedListener: clicked")
                if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQUEST_PERMISSION)
                } else {
                    otaListViewModel.connectDevice(item)
                    showProgressDialog("Connecting to GATT")

                }

            }
        }

}