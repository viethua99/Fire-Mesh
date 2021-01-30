package com.ceslab.firemesh.presentation.network

import android.view.View
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import timber.log.Timber

class NetworkFragment : BaseFragment(){
    companion object {
        const val TAG = "NetworkFragment"
    }
    override fun getResLayoutId(): Int {
        return R.layout.fragment_network
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")

    }
}