package com.ceslab.firemesh.presentation.main.activity


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ceslab.domain.model.BluetoothStatus
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseActivity
import com.ceslab.firemesh.presentation.main.fragment.MainFragment
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : BaseActivity() {

    companion object {
        const val PERMISSIONS_REQUEST_CODE: Int = 12

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
        checkPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty())) {
                    grantResults.forEach { result ->
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            finish()
                        }
                    }
                }
            }
        }
    }

    private val onBluetoothStatusObserver = Observer<BluetoothStatus> {
        Timber.d("onBluetoothStatusObserver: $it")
        when(it){
            BluetoothStatus.BLUETOOTH_STATUS_CHANGED -> showBluetoothEnableView()
            BluetoothStatus.LOCATION_STATUS_CHANGED -> showLocationEnableView()
        }
    }


    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidInjection.inject(this)

        mainActivityViewModel = ViewModelProvider(this, viewModelFactory).get(MainActivityViewModel::class.java)
        mainActivityViewModel.checkBluetoothState()
        mainActivityViewModel.bluetoothStatus.observe(this, onBluetoothStatusObserver)
    }

    private fun setupViews() {
        Timber.d("setupViews")
        replaceFragment(MainFragment(), MainFragment.TAG, R.id.container_main)
        showBluetoothEnableView()
        showLocationEnableView()
    }

    private fun checkPermissions() {
        Timber.d("checkPermissions")
        val reqPermissions = ArrayList<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            reqPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            reqPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (reqPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                reqPermissions.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        return locationManager?.let {
            it.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || it.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )
        } ?: false
    }

    private fun showBluetoothEnableView() {
        Timber.d("showBluetoothEnableView")
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            bluetooth_enable.visibility = View.VISIBLE
            bluetooth_enable_btn.setOnClickListener {
                BluetoothAdapter.getDefaultAdapter().enable()
            }
        } else {
            bluetooth_enable.visibility = View.GONE
        }
    }

    private fun showLocationEnableView() {
        Timber.d("showLocationEnableView")
        if (!isLocationEnabled()) {
            location_enable.visibility = View.VISIBLE
            location_enable_btn.setOnClickListener {
                location_enable.visibility = View.GONE
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
                if (Build.VERSION.SDK_INT >= 23)
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        PERMISSIONS_REQUEST_CODE
                    )
            }
        } else {
            location_enable.visibility = View.GONE
        }
    }
}