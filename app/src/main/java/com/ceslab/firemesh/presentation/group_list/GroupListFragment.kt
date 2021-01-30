package com.ceslab.firemesh.presentation.group_list

import android.view.View
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseFragment
import timber.log.Timber

class GroupListFragment : BaseFragment(){
    companion object {
        const val TAG = "GroupListFragment"
    }
    override fun getResLayoutId(): Int {
       return R.layout.fragment_group_list
    }

    override fun onMyViewCreated(view: View) {
       Timber.d("onMyViewCreated")
    }
}