package com.ceslab.firemesh.presentation.scan

import android.os.Handler
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.dialogs.ProvisionBottomDialog
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.splash.SplashActivity
import kotlinx.android.synthetic.main.fragment_scan.*
import timber.log.Timber

class ScanFragment : BaseFragment() {
    companion object {
        const val TAG = "ScanFragment"
    }

    private lateinit var scannerRecyclerViewAdapter: ScanRecyclerViewAdapter

    override fun getResLayoutId(): Int {
        return R.layout.fragment_scan
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupRecyclerView()
        btn_scanning.setOnClickListener {
            tv_scanning_message.text = "Looking for nearby devices..."
            Handler().postDelayed(Runnable {
                scannerRecyclerViewAdapter.setDataList(mutableListOf("A", "B", "C", "D"))
                looking_for_devices_background.visibility = View.INVISIBLE
            }, 5000)
        }
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

    private val onProvisionButtonClickedListener =
        object : BaseRecyclerViewAdapter.ItemClickListener<String> {
            override fun onClick(position: Int, item: String) {
                Timber.d("onProvisionButtonClickedListener: clicked")
                val provisionBottomDialog = ProvisionBottomDialog()
                provisionBottomDialog.show(fragmentManager!!, "ProvisionBottomDialog")
            }
        }
}