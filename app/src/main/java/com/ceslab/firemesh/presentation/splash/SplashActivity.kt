package com.ceslab.firemesh.presentation.splash

import android.os.Bundle
import android.os.Handler
import androidx.core.os.postDelayed
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseActivity
import com.ceslab.firemesh.presentation.main.MainActivity
import timber.log.Timber

class SplashActivity : BaseActivity() {
    companion object {
        const val TRANSACTION_DELAY_TIME : Long = 2000
    }
    override fun getResLayoutId(): Int {
        return R.layout.activity_splash
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        changeToMainActivityAfterDelayTime()
    }

    private fun changeToMainActivityAfterDelayTime(){
        Handler().postDelayed(Runnable {
            Timber.d("changeToMainActivityAfterDelayTime")
            MainActivity.startMainActivity(this)
        }, TRANSACTION_DELAY_TIME)
    }
}