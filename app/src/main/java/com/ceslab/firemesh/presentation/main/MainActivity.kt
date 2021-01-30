package com.ceslab.firemesh.presentation.main


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseActivity
import timber.log.Timber

class MainActivity : BaseActivity() {

    companion object {
        fun startMainActivity(activity: AppCompatActivity) {
            Timber.d("startMainActivity")
            val intent = Intent(activity, MainActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun getResLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setupViews()
    }

    private fun setupViews() {
        Timber.d("setupViews")
        replaceFragment(MainFragment(), MainFragment.TAG, R.id.container_main)
    }
}