package com.ceslab.firemesh.presentation.network

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.scan.NetworkRecyclerViewAdapter
import com.ceslab.firemesh.presentation.scan.ScanRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_scan.*
import timber.log.Timber

class NetworkFragment : BaseFragment(){
    companion object {
        const val TAG = "NetworkFragment"
    }

    private lateinit var networkRecyclerViewAdapter: NetworkRecyclerViewAdapter

    override fun getResLayoutId(): Int {
        return R.layout.fragment_network
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupRecyclerView()
    }

    private fun setupRecyclerView(){
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        networkRecyclerViewAdapter = NetworkRecyclerViewAdapter(view!!.context)
        rv_scan.layoutManager = linearLayoutManager
        rv_scan.setHasFixedSize(true)
        rv_scan.adapter = networkRecyclerViewAdapter
        networkRecyclerViewAdapter.setDataList(mutableListOf("A","B","C","D","E","F","G"))
    }
}