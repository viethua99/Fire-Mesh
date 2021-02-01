package com.ceslab.firemesh.presentation.main.activity


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseActivity
import com.ceslab.firemesh.presentation.main.fragment.MainFragment
import dagger.android.AndroidInjection
import timber.log.Timber

class MainActivity : BaseActivity() {

    companion object {
        fun startMainActivity(activity: AppCompatActivity) {
            Timber.d("startMainActivity")
            val intent = Intent(activity, MainActivity::class.java)
            activity.startActivity(intent)
        }
    }

    private lateinit var mainActivityViewModel: MainActivityViewModel

    override fun getResLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setupViewModel()
        setupViews()
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidInjection.inject(this)
        mainActivityViewModel =
            ViewModelProvider(this, viewModelFactory).get(MainActivityViewModel::class.java)
        mainActivityViewModel.status.observe(this, Observer {
           showToastMessage("Status get: $it")
        })
    }

    private fun setupViews() {
        Timber.d("setupViews")
        mainActivityViewModel.test()
        replaceFragment(MainFragment(), MainFragment.TAG, R.id.container_main)
    }
}