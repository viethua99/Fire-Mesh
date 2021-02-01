package com.ceslab.firemesh.presentation.network_list

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.network.NetworkFragment
import com.ceslab.firemesh.presentation.scan.NetworkListRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_network_list.*
import timber.log.Timber

class NetworkListFragment : BaseFragment(){
    companion object {
        const val TAG = "NetworkListFragment"
    }

    private lateinit var networkListRecyclerViewAdapter: NetworkListRecyclerViewAdapter

    override fun getResLayoutId(): Int {
        return R.layout.fragment_network_list
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupRecyclerView()
    }

    private fun setupRecyclerView(){
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        networkListRecyclerViewAdapter = NetworkListRecyclerViewAdapter(view!!.context)
        networkListRecyclerViewAdapter.itemClickListener = onNetworkItemClickedListener
        rv_network_list.layoutManager = linearLayoutManager
        rv_network_list.setHasFixedSize(true)
        rv_network_list.adapter = networkListRecyclerViewAdapter

        networkListRecyclerViewAdapter.setDataList(mutableListOf("A","B"))
    }

    private val onNetworkItemClickedListener = object : BaseRecyclerViewAdapter.ItemClickListener<String> {
        override fun onClick(position: Int, item: String) {
            Timber.d("onNetworkItemClickedListener: clicked")
            val mainActivity = activity as MainActivity
            mainActivity.replaceFragment(NetworkFragment(),NetworkFragment.TAG,R.id.container_main)
        }
    }
}