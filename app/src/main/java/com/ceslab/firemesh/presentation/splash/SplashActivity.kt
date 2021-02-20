package com.ceslab.firemesh.presentation.splash

import android.os.Bundle
import android.os.Handler
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseActivity
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import timber.log.Timber

class SplashActivity : BaseActivity() {
    companion object {
        const val TRANSACTION_DELAY_TIME : Long = 1000
    }
    override fun getResLayoutId(): Int {
        return R.layout.activity_splash
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        supportActionBar!!.hide();

        changeToMainActivityAfterDelayTime()
    }

    private fun changeToMainActivityAfterDelayTime(){
        Handler().postDelayed(Runnable {
            Timber.d("changeToMainActivityAfterDelayTime")
            MainActivity.startMainActivity(this)
        }, TRANSACTION_DELAY_TIME)
    }
}