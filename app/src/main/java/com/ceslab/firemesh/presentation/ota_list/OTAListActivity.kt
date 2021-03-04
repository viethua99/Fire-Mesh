package com.ceslab.firemesh.presentation.ota_list


import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.ota.ble.Discovery
import com.ceslab.firemesh.ota.callbacks.TimeoutGattCallback
import com.ceslab.firemesh.ota.model.BluetoothDeviceInfo
import com.ceslab.firemesh.ota.service.OTAService
import com.ceslab.firemesh.presentation.base.BaseActivity
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.node_list.OTAListRecyclerViewAdapter
import com.ceslab.firemesh.presentation.ota_setup.OTASetupActivity
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.fragment_ota_list.*
import kotlinx.android.synthetic.main.fragment_ota_list.btn_scanning
import kotlinx.android.synthetic.main.fragment_ota_list.looking_for_devices_background
import timber.log.Timber

class OTAListActivity : BaseActivity(), Discovery.BluetoothDiscoveryHost,
    Discovery.DeviceContainer<BluetoothDeviceInfo> {

    companion object {
        fun startOTAListActivity(activity: AppCompatActivity) {
            Timber.d("startOTAListActivity")
            val intent = Intent(activity, OTAListActivity::class.java)
            activity.startActivity(intent)
        }
    }

    private var service: OTAService? = null
    private var binding: OTAService.Binding? = null
    private val discovery = Discovery(this, this)
    private lateinit var handler: Handler

    private var scanning = false


    private lateinit var otaListRecyclerViewAdapter : OTAListRecyclerViewAdapter
    private lateinit var otaListViewModel: OTAListViewModel


    override fun getResLayoutId(): Int {
        return R.layout.activity_ota_list
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setupViewModel()
        setupViews()
        handler = Handler()

        bindBluetoothService()

        discovery.connect(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidInjection.inject(this)
        otaListViewModel = ViewModelProvider(this, viewModelFactory).get(OTAListViewModel::class.java)

    }

    private fun setupViews() {
        Timber.d("setupViews")
        supportActionBar?.title = "OTA List"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        btn_scanning.setOnClickListener(onScanButtonClickListener)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(this)
        otaListRecyclerViewAdapter = OTAListRecyclerViewAdapter(this)
        otaListRecyclerViewAdapter.itemClickListener = onOTAButtonClickedListener
        rv_ota_list.apply {
            layoutManager = linearLayoutManager
            setHasFixedSize(true)
            adapter = otaListRecyclerViewAdapter
        }
    }

    private val onScanButtonClickListener = View.OnClickListener {
        Timber.d("onScanButtonClickListener: clicked")
        if (scanning || btn_scanning.text == "Stop Scanning") {
            discovery.stopDiscovery(false)
            scanning = false
            btn_scanning.text =getString(R.string.fragment_provision_list_start_scanning)
            btn_scanning.setBackgroundColor(Color.parseColor("#0288D1"))
        } else {
            btn_scanning.text = getString(R.string.fragment_provision_list_stop_scanning)
            btn_scanning.setBackgroundColor(Color.parseColor("#F44336"))
            startScanning()
        }
    }

    private val onOTAButtonClickedListener =
        object : BaseRecyclerViewAdapter.ItemClickListener<BluetoothDeviceInfo> {
            override fun onClick(position: Int, item: BluetoothDeviceInfo) {
                Timber.d("onOTAButtonClickedListener: clicked")
               connectToDevice(item)
            }

            override fun onLongClick(position: Int, item: BluetoothDeviceInfo) {}
        }

    ////TEST////

    private fun connectToDevice(device:BluetoothDeviceInfo?) {
        Timber.d("connectToDevice: ${device!!.address}")
        showProgressDialog("Connecting to device")
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            hideDialog()
            return
        }

        if (scanning) {
            discovery.stopDiscovery(false)
            scanning = false
            btn_scanning.text =getString(R.string.fragment_provision_list_start_scanning)
            btn_scanning.setBackgroundColor(Color.parseColor("#0288D1"))
        }

        if (device == null) {
            Timber.e("null")
            return
        }

        val bluetoothDeviceInfo: BluetoothDeviceInfo = device

        service?.connectGatt(bluetoothDeviceInfo.device,false,object :TimeoutGattCallback() {
            override fun onTimeout() {
                Timber.d("onTimeout")
                hideDialog()
                showToastMessage("Timeout")
            }

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Timber.d("onConnectionStateChange: $status")
                if(newState == BluetoothGatt.STATE_DISCONNECTED && status != BluetoothGatt.GATT_SUCCESS) {
                    hideDialog()
                    if(status == 133) {
                        Timber.e("onConnectionStateChange: Reconnect due to 0x85 (133) error")
                        handler.postDelayed({
                            gatt.close()
                            connectToDevice(device)
                        }, 1000)
                        return

                    }
                } else if(newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    Timber.d("onConnectionStateChange: GAT_SUCCESS")
                    hideDialog()

                    service?.let {
                        if(it.isGattConnected) {
                            val intent = Intent(this@OTAListActivity, OTASetupActivity::class.java)
                            intent.putExtra("DEVICE_SELECTED_ADDRESS", device.address)
                            startActivity(intent)
                        }
                    }
                } else if(newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Timber.d("onConnectionStateChange: STATE_DISCONNECTED")
                    hideDialog()

                    gatt.close()
                    service?.clearGatt()
                }
            }
        })

    }

    private fun bindBluetoothService() {
        Timber.d("bindBluetoothService")
        binding = object : OTAService.Binding(this) {
            override fun onBound(service: OTAService?) {
                Timber.d("onBound")
                this@OTAListActivity.service = service
            }
        }
        binding?.bind()
    }

    private fun startScanning() {
        Timber.d("startScanning")
        scanning = true

        // Connected devices are not deleted from list
        reDiscover(false)
    }

    private fun reDiscover(clearCachedDiscoveries: Boolean) {
        Timber.d("reDiscover: $clearCachedDiscoveries")
        discovery.startDiscovery(clearCachedDiscoveries)
    }


    override fun isReady(): Boolean {
        return !isFinishing
    }

    override fun reDiscover() {
        Timber.d("reDiscover")
        reDiscover(false)
    }

    override fun flushContainer() {
        Timber.d("flushContainer")
    }

    override fun onAdapterDisabled() {
        Timber.d("onAdapterDisabled")
    }

    override fun onAdapterEnabled() {
        Timber.d("onAdapterEnabled")

    }

    override fun updateWithDevices(devices: List<BluetoothDeviceInfo>) {
        for(device in devices) {
            Timber.d("updateWithDevices: ${device.name} ---- ${device.address}")
        }
        looking_for_devices_background.visibility =View.GONE
        otaListRecyclerViewAdapter.setDataList(devices)
    }

}