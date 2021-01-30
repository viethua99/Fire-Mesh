package com.ceslab.firemesh.presentation.main


import android.os.Bundle
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseActivity
import timber.log.Timber

class MainActivity : BaseActivity() {

    override fun getResLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setupViews()
    }

    private fun setupViews(){
        Timber.d("setupViews")
        replaceFragment(MainFragment(),MainFragment.TAG,R.id.container_main)
    }
}