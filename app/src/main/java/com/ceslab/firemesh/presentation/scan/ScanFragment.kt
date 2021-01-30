package com.ceslab.firemesh.presentation.scan

import android.view.View
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import timber.log.Timber

class ScanFragment : BaseFragment(){
    companion object {
        const val TAG = "ScanFragment"
    }

    override fun getResLayoutId(): Int {
        return R.layout.fragment_scan
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")

    }
}