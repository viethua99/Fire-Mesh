package com.ceslab.firemesh.presentation.node

import android.view.View
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import timber.log.Timber

class NodeFragment : BaseFragment(){
    override fun getResLayoutId(): Int {
       return R.layout.fragment_node
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
    }
}