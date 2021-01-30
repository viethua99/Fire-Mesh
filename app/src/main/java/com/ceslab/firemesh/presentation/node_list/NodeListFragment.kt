package com.ceslab.firemesh.presentation.node_list

import android.view.View
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import timber.log.Timber

class NodeListFragment : BaseFragment(){
    companion object {
        const val TAG = "NodeListFragment"
    }
    override fun getResLayoutId(): Int {
       return R.layout.fragment_node_list
    }

    override fun onMyViewCreated(view: View) {
       Timber.d("onMyViewCreated")
    }
}