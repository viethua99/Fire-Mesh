package com.ceslab.firemesh.presentation.main.activity


import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.ceslab.firemesh.presentation.base.BaseActivity
import com.ceslab.firemesh.presentation.main.fragment.MainFragment
import com.ceslab.firemesh.presentation.subnet.SubnetFragment
import com.ceslab.firemesh.background_service.FireMeshService
import com.ceslab.firemesh.background_service.ScanRestartReceiver
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.ota.utils.Converters
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

     private lateinit var serviceIntent : Intent
    private lateinit var  fireMeshService: FireMeshService
    private lateinit var mainActivityViewModel: MainActivityViewModel

    override fun getResLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.container_main)
            if(currentFragment is SubnetFragment){
                currentFragment.onResume()
            }
        }
        setupViewModel()
        setupViews()
        checkPermissions()
        getExtraData()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        stopService(serviceIntent)
        val broadcastIntent = Intent()
        broadcastIntent.action = "restartService"
        broadcastIntent.setClass(this, ScanRestartReceiver::class.java)
        this.sendBroadcast(broadcastIntent)
        super.onDestroy()

    }


    private fun startFireMeshService(){
        Timber.d("startFireMeshService")
        fireMeshService = FireMeshService()
        serviceIntent = Intent(this,fireMeshService::class.java)
        if(!isServiceRunning(fireMeshService::class.java)){
            startService(serviceIntent)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean{
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for(service in manager.getRunningServices(Int.MAX_VALUE)){
            if(serviceClass.name == service.service.className){
                Timber.d("Service status: Running")
                return true
            }
        }
        Timber.d("Service status: Not running")
        return false
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
                            return
                        }
                        startFireMeshService()
                    }
                }
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (supportFragmentManager.backStackEntryCount > 0) {
            if (item.itemId == android.R.id.home) {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }





    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidInjection.inject(this)
        mainActivityViewModel =
            ViewModelProvider(this, viewModelFactory).get(MainActivityViewModel::class.java)

        mainActivityViewModel.getMeshStatus().observe(this, onGetMeshStatusObserver)
    }

    private fun setupViews() {
        Timber.d("setupViews")
        replaceFragmentWithoutAddToBackStack(MainFragment(), MainFragment.TAG, R.id.container_main)
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
        } else {
            startFireMeshService()
        }
    }

    private fun getExtraData(){
        Timber.d("getExtraData")
        val extras = intent.extras
        extras?.let {
            if(extras.containsKey(FireMeshService.FIRE_MESH_SERVICE_KEY)){
                val netKey = extras.getByteArray(FireMeshService.FIRE_MESH_SERVICE_KEY)
                netKey?.let {
                    Timber.d("netKey=${Converters.bytesToHex(netKey)}")
                 val subnet =   mainActivityViewModel.setCurrentSubnet(netKey)
                    subnet?.let {
                       replaceFragment(SubnetFragment(it.name),SubnetFragment.TAG,R.id.container_main)
                    }
                }



            }
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


    private val onGetMeshStatusObserver = Observer<MeshStatus> {
        it.let {
            when(it) {
                MeshStatus.BLUETOOTH_STATE_CHANGED -> showBluetoothEnableView()
                MeshStatus.LOCATION_STATE_CHANGED -> showLocationEnableView()
            }
        }
    }
}