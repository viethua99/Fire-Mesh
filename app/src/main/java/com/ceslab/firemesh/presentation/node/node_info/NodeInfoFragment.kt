package com.ceslab.firemesh.presentation.node.node_info

import android.view.View
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import timber.log.Timber

class NodeInfoFragment : BaseFragment(){
    override fun getResLayoutId(): Int {
        return R.layout.fragment_node_info
    }

    override fun onMyViewCreated(view: View) {
       Timber.d("onMyViewCreated")
    }
}