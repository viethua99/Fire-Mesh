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
import android.view.Menu
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
import com.ceslab.firemesh.service.FireMeshService
import com.ceslab.firemesh.service.ScanRestartReceiver
import com.ceslab.firemesh.ota.utils.Converters
import com.ceslab.firemesh.presentation.ota_list.OTAListActivity
import com.siliconlabs.bluetoothmesh.App.AESUtils
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.lang.Exception

class MainActivity : BaseActivity() {

    companion object {
        const val PERMISSIONS_REQUEST_CODE: Int = 12
        private const val WRITE_EXTERNAL_STORAGE_REQUEST_PERMISSION = 300

        fun startMainActivity(activity: AppCompatActivity) {
            Timber.d("startMainActivity")
            val intent = Intent(activity, MainActivity::class.java)
            activity.startActivity(intent)
        }
    }

    private lateinit var serviceIntent: Intent
    private lateinit var mainActivityViewModel: MainActivityViewModel
    private var fireMeshService: FireMeshService? = null

    override fun getResLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.container_main)
            if (currentFragment is SubnetFragment) {
                currentFragment.onResume()
            }
        }
        setupViewModel()
        setupViews()
        checkPermissions()
        getExtraData()
        testData()
    }

    fun testData(){
        //MAX DATA PACKET = 192 Bytes
       val rawData = byteArrayOf(
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20,
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40,
            0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x50,

            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20,
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40,
            0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x50,

            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20,
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40,
            0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x50,

            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20,
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40,
            0x41, 0x42
        )

       val aesKey = byteArrayOf(
            0xFF.toByte(),
            0x00.toByte(),
            0xFF.toByte(),
            0x00.toByte(),
            0xFF.toByte(),
            0x00.toByte(),
            0xFF.toByte(),
            0x00.toByte(),
            0xFF.toByte(),
            0x00.toByte(),
            0xFF.toByte(),
            0x00.toByte(),
            0xFF.toByte(),
            0x00.toByte(),
            0xFF.toByte(),
            0x00.toByte()
        )

        try {
            val encryptTest = byteArrayOf(
                0x30, 0x98.toByte(), 0x24, 0xe7.toByte(),
                0xf5.toByte(), 0x27, 0xf3.toByte(), 0xa8.toByte(), 0x74, 0x5c,
                0x81.toByte(), 0xed.toByte(), 0x32, 0x6d, 0x77, 0x0b,
                0xa8.toByte(), 0xbf.toByte(), 0x82.toByte(), 0xd9.toByte(),
                0x2c, 0x7e, 0xb6.toByte(), 0x35, 0x72, 0x64, 0xef.toByte(), 0x78,
                0xab.toByte(), 0x29,
                0x5b, 0xe3.toByte(), 0x47, 0x40, 0xcd.toByte(), 0x09, 0x3c,
                0xff.toByte(), 0xc6.toByte(), 0xe9.toByte(),
                0x28, 0xe6.toByte(), 0x86.toByte(),
                0xec.toByte(), 0x83.toByte(), 0x35, 0x50, 0x37, 0x66, 0x12,

                0xfd.toByte(), 0x73, 0xf4.toByte(), 0x2b,
                0xff.toByte(), 0xad.toByte(), 0xe5.toByte(), 0x18, 0xe3.toByte(), 0xec.toByte(),
                0xe2.toByte(), 0x7f, 0x07, 0x23, 0xf0.toByte(),
                0xdb.toByte(), 0x3c, 0x59, 0x1d, 0x8a.toByte(),
                0xb9.toByte(), 0x16, 0x73, 0xc2.toByte(),
                0xac.toByte(), 0xbf.toByte(), 0xb0.toByte(), 0xfe.toByte(), 0x03, 0xdd.toByte(),
                0xcf.toByte(), 0x89.toByte(),
                0xd7.toByte(), 0xb2.toByte(), 0x88.toByte(), 0x16, 0xe6.toByte(), 0x12,
                0xa7.toByte(), 0xa1.toByte(),
                0x1c, 0x61, 0x38, 0x48, 0x74, 0xcc.toByte(), 0xb2.toByte(),
                0xa1.toByte(), 0x2d, 0x11,

                0x77, 0x59, 0x6c, 0x4c, 0x33, 0xf8.toByte(), 0x30, 0x88.toByte(),
                0xaa.toByte(), 0xf9.toByte(),
                0xb1.toByte(), 0xf7.toByte(), 0x2b,
                0xf2.toByte(), 0x8a.toByte(), 0xdb.toByte(), 0xfd.toByte(), 0x63,
                0xdf.toByte(), 0xdf.toByte(),
                0xdd.toByte(), 0xbf.toByte(),
                0xc1.toByte(), 0x95.toByte(), 0x68, 0xa2.toByte(), 0xd9.toByte(),
                0xc6.toByte(), 0xe4.toByte(), 0x2b,
                0x4c, 0x95.toByte(), 0x77, 0xef.toByte(), 0x1e,
                0xe7.toByte(), 0x9d.toByte(), 0x1c, 0xa3.toByte(), 0x95.toByte(),
                0x02, 0x9d.toByte(), 0x97.toByte(),
                0xbf.toByte(), 0xad.toByte(), 0x47, 0xc1.toByte(), 0x2c, 0x6c, 0x06,

                0x38, 0xb5.toByte(), 0x1c, 0x61, 0xae.toByte(), 0x38, 0x48, 0x36,
                0xcd.toByte(), 0x1a,
                0x23, 0x41, 0xc9.toByte(), 0xac.toByte(), 0x1d,
                0xb8.toByte(), 0x9c.toByte(), 0xc9.toByte(), 0xeb.toByte(), 0xa0.toByte(),
                0xf4.toByte(), 0x7d, 0x68, 0xf3.toByte(), 0x21,
                0xc8.toByte(), 0x2b, 0xbb.toByte(), 0x87.toByte(), 0x6d,
                0x1e, 0x27, 0xee.toByte(), 0x9e.toByte(),
                0xa9.toByte(), 0x93.toByte(), 0xeb.toByte(), 0x35, 0x8a.toByte(), 0xff.toByte(),
                0x32, 0x3d)
            val encryptedData =  AESUtils.encrypt(AESUtils.ECB_ZERO_BYTE_PADDING_ALGORITHM,aesKey,rawData)
//            Timber.d("Test:encryptedData= " + Converters.bytesToHexWhitespaceDelimited(encryptedData))
            val decriptedData =  AESUtils.decrypt(AESUtils.ECB_ZERO_BYTE_PADDING_ALGORITHM,aesKey,encryptTest)
            Timber.d("Test:decriptedData= " + Converters.bytesToHexWhitespaceDelimited(decriptedData))
        } catch (exception: Exception){
            exception.printStackTrace()
        }


    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        if (isServiceRunning(fireMeshService!!::class.java)) {
            stopService(serviceIntent)
            val broadcastIntent = Intent()
            broadcastIntent.action = "restartService"
            broadcastIntent.setClass(this, ScanRestartReceiver::class.java)
            this.sendBroadcast(broadcastIntent)
        }
        super.onDestroy()

    }


    private fun triggerFireMeshService() {
        Timber.d("triggerFireMeshService")
        fireMeshService = FireMeshService()
        serviceIntent = Intent(this, fireMeshService!!::class.java)
        if (!isServiceRunning(fireMeshService!!::class.java)) {
            startService(serviceIntent)
        } else {
            stopService(serviceIntent)
        }
    }

     fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
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
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val item = menu?.findItem(R.id.item_background_scan)
        item?.let {
            if (fireMeshService != null) {
                if (isServiceRunning(fireMeshService!!::class.java)) {
                    item.title = "Stop Background"
                } else {
                    item.title = "Start Background"
                }
            }
        }
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (supportFragmentManager.backStackEntryCount > 0) {
            if (item.itemId == android.R.id.home) {
                onBackPressed()
                return true
            }
        } else {
            when (item.itemId) {
                R.id.item_ota -> {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissions(
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            WRITE_EXTERNAL_STORAGE_REQUEST_PERMISSION
                        )
                    } else {
                        OTAListActivity.startOTAListActivity(this)
                    }
                }
                R.id.item_background_scan -> {
                    if (isLocationEnabled() && BluetoothAdapter.getDefaultAdapter().isEnabled) {
                        triggerFireMeshService()
                        if (fireMeshService != null) {
                            if (isServiceRunning(fireMeshService!!::class.java)) {
                                item.title = "Stop Background"
                            } else {
                                item.title = "Start Background"
                            }
                        }
                    } else {
                        showToastMessage("Cannot start background scan")
                    }
                }
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
        }
    }

    private fun getExtraData() {
        Timber.d("getExtraData")
        val extras = intent.extras
        extras?.let {
            if (extras.containsKey(FireMeshService.FIRE_MESH_SERVICE_KEY)) {
                stopService(serviceIntent) // User turned on app by clicked notification, so we can stop background scan from now
                val netKey = extras.getByteArray(FireMeshService.FIRE_MESH_SERVICE_KEY)
                netKey?.let {
                    Timber.d("netKey=${Converters.bytesToHex(netKey)}")
                    val subnet = mainActivityViewModel.setCurrentSubnet(netKey)
                    subnet?.let {
                        replaceFragment(
                            SubnetFragment(it.name),
                            SubnetFragment.TAG,
                            R.id.container_main
                        )
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
            when (it) {
                MeshStatus.BLUETOOTH_STATE_CHANGED -> {
                    showBluetoothEnableView()
                    if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
                        if (isServiceRunning(fireMeshService!!::class.java)) {
                            stopService(serviceIntent)
                        }
                    }
                }
                MeshStatus.LOCATION_STATE_CHANGED -> {
                    showLocationEnableView()
                    if (!isLocationEnabled()) {
                        if (isServiceRunning(fireMeshService!!::class.java)) {
                            stopService(serviceIntent)
                        }
                    }
                }
            }
        }
    }
}