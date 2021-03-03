package com.ceslab.firemesh.presentation.ota_setup

import android.app.Activity
import android.app.Dialog
import android.bluetooth.*
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.OpenableColumns
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ceslab.firemesh.R
import com.ceslab.firemesh.ota.model.OTAType
import com.ceslab.firemesh.ota.callbacks.TimeoutGattCallback
import com.ceslab.firemesh.ota.service.OTAService
import com.ceslab.firemesh.presentation.base.BaseActivity
import com.ceslab.firemesh.presentation.ota_setup.dialog.ErrorDialog
import kotlinx.android.synthetic.main.activity_ota_setup.*
import timber.log.Timber
import java.io.*
import java.lang.reflect.Method
import java.util.*

class OTASetupActivity : BaseActivity() {

    companion object {
        private const val FILE_CHOOSER_REQUEST_CODE = 9999
        private const val UI_CREATION_DELAY = 0
        private const val RECONNECTION_RETRIES = 3
        private const val GATT_FETCH_ON_SERVICE_DISCOVERED_DELAY = 875

        var ota_service = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0")
        private val ota_control = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063")
        private val ota_data = UUID.fromString("984227f3-34fc-4045-a5d0-2c581f81a153")
        private val homekit_descriptor = UUID.fromString("dc46f0fe-81d2-4616-b5d9-6abdd796939a")
        private val homekit_service = UUID.fromString("0000003e-0000-1000-8000-0026bb765291")

        fun startOTASetupActivity(activity: AppCompatActivity) {
            Timber.d("startOTASetupActivity")
            val intent = Intent(activity, OTASetupActivity::class.java)
            activity.startActivity(intent)
        }
    }

    //TEST
    private var disconnect_gatt = false
    private var kit_descriptor: BluetoothGattDescriptor? = null
    private var doubleStepUpload = false
    private var otafile: ByteArray? = null
    private var pack = 0
    private var boolOTAdata = false
    private var delayNoResponse = 1
    private var ota_process = false
    private var connected = false
    private var discoverTimeout = true
    private var UICreated = false

    private var retryAttempts = 0
    private val DFU_OTA_UPLOAD = Runnable { dfuMode("OTAUPLOAD") }
    private val WRITE_OTA_CONTROL_ZERO = Runnable { writeOtaControl(0x00.toByte()) }
    private var homekit = false

    private var loadingdialog: Dialog? = null
    private var loadingLog: TextView? = null
    private var loadingHeader: TextView? = null
    private var loadingimage: ProgressBar? = null

    private var errorDialog: ErrorDialog? = null

    private lateinit var handler: Handler
    private var bluetoothBinding: OTAService.Binding? = null
    private var service: OTAService? = null
    private var serviceHasBeenSet = false
    var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var disconnectionTimeout = false
    private var otaMode = false
    private var ota_mode = false
    private var boolFullOTA = false
    private var boolOTAbegin = false
    private var mtuDivisible = 0
    private var otatime: Long = 0


    // OTA file paths
    private var applicationPath = ""
    private var stackPath = ""
    private var isOTAInit: Boolean = false
    private var currentMTU = 247
    private var currentPriority = 2
    private var isReliable = true


    override fun getResLayoutId(): Int {
        return R.layout.activity_ota_setup
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        otaMode = true
        initDevice("")
        setupViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingdialog?.dismiss()
        bluetoothBinding?.unbind()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                FILE_CHOOSER_REQUEST_CODE -> {
                    val uri = data?.data
                    val filename: String?

                    filename = try {
                        getFileName(uri)
                    } catch (e: Exception) {
                        ""
                    }

                    if (!hasOtaFileCorrectExtension(filename)) {
                        return
                    }
                    btn_select_application_gbl_file.text = filename
                    prepareOtaFile(uri, filename)
                }
            }
        }

        if (arePartialOTAFilesCorrect()) {
            btn_ota_proceed.isClickable = true
            btn_ota_proceed.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_color))
        } else {
            btn_ota_proceed.isClickable = false
            btn_ota_proceed.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_7))
        }
    }

    /**
     * INITIALIZES LOADING DIALOG
     */
    private fun initLoading() {
        loadingdialog = Dialog(this)
        loadingdialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        loadingdialog?.setContentView(R.layout.dialog_ota_loading)

        loadingimage = loadingdialog?.findViewById(R.id.connecting_spinner)
        loadingLog = loadingdialog?.findViewById(R.id.loadingLog)
        loadingHeader = loadingdialog?.findViewById(R.id.loading_header)
    }

    private fun setupViews() {
        Timber.d("setupViews")
        btn_ota_proceed.apply {
            setBackgroundColor(ContextCompat.getColor(this@OTASetupActivity, R.color.gray_7))
            setOnClickListener(onProceedButtonClicked)
        }

        btn_select_application_gbl_file.setOnClickListener(onSelectApplicationFileClicked)

        btn_partial_ota.apply {
            backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.button_clicked_color))
            setOnClickListener(onPartialOTAButtonClicked)
        }


        btn_full_ota.setOnClickListener(onFullOTAButtonClicked)

        edt_mtu_value.setOnEditorActionListener(onMaxMTUValueEdited)

        seekbar_mtu.apply {
            max = 250 - 23
            progress = 250 - 23
            setOnSeekBarChangeListener(onMTUBarChanged)
        }

        seekbar_priority.apply {
            max = 2
            progress = 2
            setOnSeekBarChangeListener(onPriorityBarChanged)
        }

        rdb_reliability.setOnClickListener { isReliable = true }

        rdb_speed.setOnClickListener { isReliable = false }
    }

    private fun changeOTATypeView(otaType: OTAType) {
        Timber.d("changeOTATypeView: $otaType")
        when (otaType) {
            OTAType.PARTIAL_OTA -> {
                btn_partial_ota.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.button_clicked_color))
                btn_full_ota.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.primary_color))
                layout_app_loader.visibility = View.GONE
            }

            OTAType.FULL_OTA -> {
                btn_full_ota.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.button_clicked_color))
                btn_partial_ota.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.primary_color))
                layout_app_loader.visibility = View.VISIBLE
            }
        }
    }


    private fun arePartialOTAFilesCorrect(): Boolean {
        Timber.d("arePartialOTAFilesCorrect")
        return btn_select_application_gbl_file.text != "Select Application .gbl file"
    }

    private fun getFileName(uri: Uri?): String? {
        Timber.d("getFileName")
        var result: String? = null
        if ((uri?.scheme == "content")) {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor.use { c ->
                if (c != null && c.moveToFirst()) {
                    result = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri?.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }

    fun hasOtaFileCorrectExtension(filename: String?): Boolean {
        Timber.d("hasOtaFileCorrectExtension")
        return filename?.toUpperCase(Locale.getDefault())?.contains(".GBL")!!
    }

    fun prepareOtaFile(uri: Uri?, name: String?) {
        Timber.d("prepareOtaFile")
        try {
            val inStream = contentResolver.openInputStream(uri!!) ?: return
            val file = File(cacheDir, name)
            val output: OutputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while ((inStream.read(buffer).also { read = it }) != -1) {
                output.write(buffer, 0, read)
            }
            applicationPath = file.absolutePath
            output.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private val onProceedButtonClicked = View.OnClickListener {
        Timber.d("onProceedButtonClicked")
        btn_ota_proceed.isClickable = false
        if (ota_mode) {
            bluetoothGatt?.requestMtu(edt_mtu_value?.text.toString().toInt())
        } else dfuMode("OTABEGIN")
    }


    private val onSelectApplicationFileClicked = View.OnClickListener {
        Timber.d("onSelectApplicationFileClicked")
        val intent = Intent().apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(
            Intent.createChooser(intent, "Choose directory"),
            FILE_CHOOSER_REQUEST_CODE
        )
    }

    private val onPartialOTAButtonClicked = View.OnClickListener {
        Timber.d("onPartialOTAButtonClicked")
        changeOTATypeView(OTAType.PARTIAL_OTA)
        doubleStepUpload = false

    }

    private val onFullOTAButtonClicked = View.OnClickListener {
        Timber.d("onFullOTAButtonClicked")
        changeOTATypeView(OTAType.FULL_OTA)
        doubleStepUpload = true

    }

    private val onMaxMTUValueEdited = TextView.OnEditorActionListener { _, _, _ ->
        if (edt_mtu_value.text != null) {
            var mtuValue = edt_mtu_value.text.toString().toInt()
            if (mtuValue < 23) mtuValue = 23 else if (mtuValue > 250) mtuValue = 250
            seekbar_mtu.progress = mtuValue - 23
            currentMTU = mtuValue
        }
        false
    }

    private val onMTUBarChanged = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            Timber.d("onProgressChanged: $progress")
            edt_mtu_value.setText(" ${progress + 23}")
            currentMTU = progress + 23
        }
    }

    private val onPriorityBarChanged = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            Timber.d("onProgressChanged: $progress")
            when (progress) {
                1 -> currentPriority = 0 //BALANCE
                2 -> currentPriority = 1 //HIGH
                0 -> currentPriority = 2 //LOW
            } //LOW
        }
    }

    //TEST OTA ////
    private fun initDevice(deviceAddress: String?) {
        Log.d("Test", "initDevice: $deviceAddress")
        bluetoothBinding = object : OTAService.Binding(this) {
            override fun onBound(service: OTAService?) {
                serviceHasBeenSet = true
                this@OTASetupActivity.service = service
                if (!service?.isGattConnected(deviceAddress)!!) {
                    disconnectGatt(bluetoothGatt)
                } else {
                    val bG = service.getConnectedGatt(deviceAddress)
                    if (bG == null) {
                        finish()
                        return
                    }

                    service.registerGattCallback(true, gattCallback)
                    if (bG.services != null && bG.services.isNotEmpty()) {
                        bluetoothGatt = bG
                        onGattFetched()
                    } else {
                        bG.discoverServices()
                    }
                }
            }
        }
        handler.postDelayed(
            { runOnUiThread { bluetoothBinding?.bind() } },
            UI_CREATION_DELAY.toLong()
        )
    }

    /**
     * SETS ALL THE INFO IN THE OTA PROGRESS DIALOG TO "" OR 0
     */
    fun resetOTAProgress() {
        Log.d("Test","resetOTAProgress")
        boolFullOTA = false
        runOnUiThread {
//            datasize?.text = ""
//            filename?.text = ""
            loadingimage?.visibility = View.GONE
            loadingdialog?.dismiss()
//            progressBar?.progress = 0
//            datasize?.text = resources.getString(R.string.zero_percent)
//            dataRate?.text = ""
//            OTAStart?.isClickable = false
//            OTAStart?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_button_inactive))
        //    showOtaProgress()
        }
    }


    /**
     * INITILIAZES ALL NECESSARY DIALOGS AND VIEW IN UI - ONCREATE
     */
    private fun onGattFetched() {
        var deviceName = bluetoothGatt?.device?.name
        supportActionBar?.title = deviceName

        UICreated = true
        if (!boolOTAbegin) {
            initLoading()
        }

    }

    fun disconnectGatt(gatt: BluetoothGatt?) {
        boolFullOTA = false
        boolOTAbegin = false
        ota_process = false
        disconnect_gatt = true
//        UICreated = false
        val disconnectTimer = Timer()

        if (gatt != null && gatt.device != null) {

            val btGatt: BluetoothGatt = gatt
            disconnectTimer.schedule(object : TimerTask() {
                override fun run() {
                    /**Getting bluetoothDevice to FetchUUID */
                    if (btGatt.device != null) bluetoothDevice = btGatt.device
                    /**Disconnect gatt */
                    btGatt.disconnect()
                    service?.clearGatt()
                    Log.d("disconnectGatt", "gatt disconnect")
                }
            }, 200)
            disconnectTimer.schedule(object : TimerTask() {
                override fun run() {
                    bluetoothDevice?.fetchUuidsWithSdp()
                }
            }, 300)
            disconnectionTimeout = true
            val timeout = Runnable {
                handler.postDelayed({
                    if (disconnectionTimeout) {
                        finish()
                    }
                }, 5000)
            }
            Thread(timeout).start()
        } else {
            finish()
        }
    }

    /**
     * WRITES BYTE TO OTA CONTROL CHARACTERISTIC
     */
    private fun writeOtaControl(ctrl: Byte): Boolean {
        Log.d("writeOtaControl", "Called")
        if (bluetoothGatt?.getService(ota_service) != null) {
            val charac = bluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_control)
            if (charac != null) {
                Log.d("Instance ID", "" + charac.instanceId)
                charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                Log.d("charac_properties", "" + charac.properties)
                val control = ByteArray(1)
                control[0] = ctrl
                charac.value = control
                bluetoothGatt?.writeCharacteristic(charac)
                return true
            } else {
                Log.d("characteristic", "null")
            }
        } else {
            Log.d("service", "null")
        }
        return false
    }

    private val gattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (!otaMode) {
                super.onReadRemoteRssi(gatt, rssi, status)
            }
        }

        override fun onTimeout() {
            super.onTimeout()
            Log.d("gattCallback", "onTimeout")
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d("Test", "onMtuChanged: $mtu - status: $status")
            if (status == 0) { //NO ERRORS
                currentMTU = mtu
                bluetoothGatt?.requestConnectionPriority(currentPriority)
                 if (ota_process) {

                    if (ota_mode) { //Reset OTA Progress
                        Log.d("Test", "onchangedMtu succeed : otamode=$otaMode")
                        resetOTAProgress()
                    }
                }
            } else { //ERROR HANDLING
                Log.d("RequestMTU", "Error: $status")
                handler.postDelayed({ disconnectGatt(bluetoothGatt) }, 2000)
            }
        }

        //CALLBACK ON CONNECTION STATUS CHANGES
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (bluetoothGatt != null) {
                if (bluetoothGatt?.device?.address != gatt.device.address) {
                    return
                }
            }
            super.onConnectionStateChange(gatt, status, newState)
            Log.d("onConnectionStateChange", "status = $status - newState = $newState")
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    connected = true
                    Log.d("onConnectionStateChange", "CONNECTED")
                    runOnUiThread {
                        if (!loadingdialog?.isShowing!!) {
//                            showMessage("DEVICE CONNECTED")
                        }
                    }
                    if (ota_process) { //After OTA process started
                        Log.d("Address", "" + gatt.device)
                        Log.d("Name", "" + gatt.device.name)
                        if (gatt.services.isEmpty()) {
                            handler.postDelayed({
                                bluetoothGatt = null //It's going to be equal gatt in Discover Services Callback...
                                Log.d(
                                    "onConnected",
                                    "Start Services Discovery: " + gatt.discoverServices()
                                )
                            }, 250)
                            discoverTimeout = true
                            val timeout = Runnable {
                                handler.postDelayed({
                                    if (discoverTimeout) {
                                        disconnectGatt(gatt)
//                                        runOnUiThread { showMessage("DISCOVER SERVICES TIMEOUT") }
                                    }
                                }, 25000)
                            }
                            Thread(timeout).start()
                        }
                    }
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    if (status == 133 && otaMode && retryAttempts < RECONNECTION_RETRIES) {
                        retryAttempts++
                        Log.d(
                            "onConnectionStateChange",
                            "[DeviceServices]: Reconnect due to 0x85 (133) error"
                        )
                        reconnect(1000)
                        return
                    }
                    connected = false
                    discoverTimeout = false
                    disconnectionTimeout = false
                    if ((status != 0) && otaMode && (errorDialog == null)) {
                        runOnUiThread {
                            errorDialog = ErrorDialog(status, object : ErrorDialog.OtaErrorCallback {
                                override fun onDismiss() {
                                    exit(bluetoothGatt)
                                }
                            })
                            errorDialog?.show(supportFragmentManager, "ota_error_dialog")
                        }
                    } else {
                        if (disconnect_gatt) {
                            exit(gatt)
                        }
                        if (ota_process || boolOTAbegin || boolFullOTA) {
                            runOnUiThread {
                                if (loadingdialog?.isShowing!!) {
                                    loadingLog?.text = "Rebooting..."
                                    handler.postDelayed({ runOnUiThread { loadingLog?.text = "Waiting..." } }, 1500)
                                }
                            }
                        }
//                        if (otaSetup != null) if (otaSetup?.isShowing!!) {
//                            exit(gatt)
//                        }
                        if (gatt.services.isEmpty()) {
                            exit(gatt)
                        }
                        if (!boolFullOTA && !boolOTAbegin && !ota_process) {
                            exit(gatt)
                        }
                    }
                }
                BluetoothGatt.STATE_CONNECTING -> Log.d("onConnectionStateChange", "Connecting...")
            }
        }

        //CALLBACK ON CHARACTERISTIC READ
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic === (bluetoothGatt?.getService(ota_service)
                    ?.getCharacteristic(ota_control))
            ) {
                val value = characteristic.value
                if (value[2] == 0x05.toByte()) {
                    Log.d("homekit_descriptor", "Insecure Connection")
//                    runOnUiThread { showMessage("Error: Not a Homekit Secure Connection") }
                } else if (value[2] == 0x04.toByte()) {
                    Log.d("homekit_descriptor", "Wrong Address")
                } else if (value[2] == 0x00.toByte()) {
                    Log.d("homekit_descriptor", "Entering in DFU_Mode...")
                    if (ota_mode && ota_process) {
                        Log.d("OTAUPLOAD", "Sent")
//                        runOnUiThread(checkbeginrunnable)
                        handler.removeCallbacks(DFU_OTA_UPLOAD)
                        handler.postDelayed(DFU_OTA_UPLOAD, 500)
                    } else if (!ota_mode && ota_process) {
                        runOnUiThread {
                            loadingLog?.text = "Resetting..."
//                            showLoading()
//                            animaloading()
//                            Constants.ota_button?.isVisible = true
                        }
                        handler.postDelayed({ reconnect(4000) }, 200)
                    }
                }
            }
        }

        //CALLBACK ON CHARACTERISTIC WRITE (PROPERTY: WHITE)
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {

            if (status != 0) { // Error Handling
                Log.d("onCharWrite", "status: " + Integer.toHexString(status))
                if (errorDialog == null) {
                    runOnUiThread {
                        errorDialog = ErrorDialog(status, object : ErrorDialog.OtaErrorCallback {
                            override fun onDismiss() {
                                exit(bluetoothGatt)
                            }
                        })
                        errorDialog?.show(supportFragmentManager, "ota_error_dialog")
                    }
                }
            } else {
                if ((characteristic.uuid == ota_control)) { //OTA Control Callback Handling
                    if (characteristic.value.size == 1) {
                        if (characteristic.value[0] == 0x00.toByte()) {
                            if (ota_mode && ota_process) {
                                Log.d("OTAUPLOAD", "Sent")
                                //  runOnUiThread(checkbeginrunnable)
                                handler.removeCallbacks(DFU_OTA_UPLOAD)
                                handler.postDelayed(DFU_OTA_UPLOAD, 500)
                            } else if (!ota_mode && ota_process) {
                                runOnUiThread {
                                    loadingLog?.text = "Resetting..."
//                                    showLoading()
//                                    animaloading()
//                                    Constants.ota_button?.isVisible = true
                                }
                                handler.post { reconnect(4000) }
                            }
                        }
                        if (characteristic.value[0] == 0x03.toByte()) {
                            if (ota_process) {
                                runOnUiThread {
                                    btn_ota_proceed?.setBackgroundColor(ContextCompat.getColor(this@OTASetupActivity, R.color.primary_color))
                                    btn_ota_proceed?.isClickable = true
                                }
                                boolOTAbegin = false
                                if (boolFullOTA) {
                                    stackPath = ""
                                    runOnUiThread {
//                                        otaProgress?.dismiss()
                                        loadingLog?.text = "Loading"
//                                        showLoading()
//                                        animaloading()
                                    }
                                    handler.postDelayed({ reconnect(4000) }, 500)
                                }
                            }
                        }
                    } else {
                        if (characteristic.value[0] == 0x00.toByte() && characteristic.value[1] == 0x02.toByte()) {
                            Log.i("HomeKit", "Reading OTA_Control...")
                            bluetoothGatt?.readCharacteristic(characteristic)
                        }
                    }
                }
                if ((characteristic.uuid == ota_data)) {   //OTA Data Callback Handling
                    if (isReliable) {
                        // if (otaProgress?.isShowing!!) {
                        pack += mtuDivisible
                        if (pack <= otafile?.size!! - 1) {
                            otaWriteDataReliable()
                        } else if (pack > otafile?.size!! - 1) {
                            handler.post {
                                runOnUiThread {
//                                        chrono?.stop()
//                                        uploadimage?.clearAnimation()
//                                        uploadimage?.visibility = View.INVISIBLE
                                }
                            }
                            boolOTAdata = false
                            retryAttempts = 0
                            dfuMode("OTAEND")
                        }
                        //  }
                    }
                }
            }
            bluetoothGatt?.readCharacteristic(characteristic)
        }

        //CALLBACK ON DESCRIPTOR WRITE
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            runOnUiThread {
                updateDescriptorView(descriptor)
            }
        }

        //CALLBACK ON DESCRIPTOR READ
        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if ((descriptor.uuid.toString() == homekit_descriptor.toString())) {
                val value = ByteArray(2)
                value[0] = 0xF2.toByte()
                value[1] = 0xFF.toByte()
                if (descriptor.value[0] == value[0] && descriptor.value[1] == value[1]) {
                    homeKitOTAControl(descriptor.value)
                }
            } else {
                runOnUiThread {
                    updateDescriptorView(descriptor)
                }
            }
        }

        @UiThread
        private fun updateDescriptorView(descriptor: BluetoothGattDescriptor) {

        }

        //CALLBACK ON CHARACTERISTIC CHANGED VALUE (READ - CHARACTERISTIC NOTIFICATION)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
//            for (key: Int in characteristicFragments.keys) {
//                val fragment = characteristicFragments[key]
//                if (fragment != null && (fragment.mBluetoothCharact?.uuid == characteristic.uuid)) {
//                    fragment.onActionDataAvailable(characteristic.uuid.toString())
//                    break
//                }
//            }
        }

        //CALLBACK ON SERVICES DISCOVERED
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (bluetoothGatt != gatt) {
                bluetoothGatt = gatt
                refreshServices()
            } else {
                discoverTimeout = false
                /**ERROR IN SERVICE DISCOVERY */
                if (status != 0) {
                    Log.d("Error status", "" + Integer.toHexString(status))
                    if (errorDialog == null) {
                        runOnUiThread {
                            errorDialog = ErrorDialog(status, object : ErrorDialog.OtaErrorCallback {
                                override fun onDismiss() {
                                    exit(bluetoothGatt)
                                }
                            })
                            errorDialog?.show(supportFragmentManager, "ota_error_dialog")
                        }
                    }
                } else {
                    /**ON SERVICE DISCOVERY WITHOUT ERROR */
                    getServicesInfo(gatt) //SHOW SERVICES IN LOG


                    //DEFINE IF DEVICE SUPPORT OTA & MODE (NORMAL/DFU)
                    val otaServiceCheck = gatt.getService(ota_service) != null
                    if (otaServiceCheck) {
                        val otaDataCheck =
                            gatt.getService(ota_service).getCharacteristic(ota_data) != null
                        if (otaDataCheck) {
                            val homekitCheck = gatt.getService(homekit_service) != null
                            if (!homekitCheck) {
                                ota_mode = true
                                val otaDataProperty = gatt.getService(ota_service)
                                    .getCharacteristic(ota_data).properties
                                if ((otaDataProperty == 12) || (otaDataProperty == 8) || (otaDataProperty == 10)) {
                                    //reliable = true;
                                } else if (ota_mode && otaDataProperty == 4) {
                                    //reliable = false;
                                }
                            }
                        } else {
                            if (boolOTAbegin) onceAgain()
                        }
                    }

                    //REQUEST MTU
                    if (UICreated && loadingdialog?.isShowing!!) {
                        bluetoothGatt?.requestMtu(currentMTU)
                    }

                    //LAUNCH SERVICES UI
                    if (!boolFullOTA) {
                        handler.postDelayed({
                            runOnUiThread {
                                onGattFetched()
                            }
                        }, GATT_FETCH_ON_SERVICE_DISCOVERED_DELAY.toLong())
                    }

                    //IF DFU_MODE, LAUNCH OTA SETUP AUTOMATICALLY
                    if (ota_mode && boolOTAbegin) {
                        handler.postDelayed({
                            runOnUiThread {
                                loadingimage?.visibility = View.GONE
                                loadingdialog?.dismiss()
                           //     showOtaProgress()
                            }
                        }, (2.5 * UI_CREATION_DELAY).toLong())
                    }

                }
            }
        }
    }

    fun onceAgain() {
        writeOtaControl(0x00.toByte())
    }

    /**
     * READ ALL THE SERVICES, PRINT IT ON LOG AND RECOGNIZES HOMEKIT ACCESSORIES
     */
    fun getServicesInfo(gatt: BluetoothGatt) {
        val gattServices = gatt.services
        Log.i("onServicesDiscovered", "Services count: " + gattServices.size)
        for (gattService: BluetoothGattService in gattServices) {
            val serviceUUID = gattService.uuid.toString()
            Log.i(
                "onServicesDiscovered",
                "Service UUID " + serviceUUID + " - Char count: " + gattService.characteristics.size
            )
            val gattCharacteristics = gattService.characteristics
            for (gattCharacteristic: BluetoothGattCharacteristic in gattCharacteristics) {
                val characteristicUUID = gattCharacteristic.uuid.toString()
                Log.i(
                    "onServicesDiscovered",
                    "Characteristic UUID " + characteristicUUID + " - Properties: " + gattCharacteristic.properties
                )
                if ((gattCharacteristic.uuid.toString() == ota_control.toString())) {
                    if (gattCharacteristics.contains(
                            bluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_data)
                        )
                    ) {
                        if (!gattServices.contains(bluetoothGatt?.getService(homekit_service))) {
                            Log.i("onServicesDiscovered", "Device in DFU Mode")
                        } else {
                            Log.i("onServicesDiscovered", "OTA_Control found")
                            val gattDescriptors = gattCharacteristic.descriptors
                            for (gattDescriptor: BluetoothGattDescriptor in gattDescriptors) {
                                val descriptor = gattDescriptor.uuid.toString()
                                if ((gattDescriptor.uuid.toString() == homekit_descriptor.toString())) {
                                    kit_descriptor = gattDescriptor
                                    Log.i("descriptor", "UUID: $descriptor")
                                    //bluetoothGatt.readDescriptor(gattDescriptor);
                                    val stable = byteArrayOf(0x00.toByte(), 0x00.toByte())
                                    homeKitOTAControl(stable)
                                    homekit = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * USED TO CLEAN CACHE AND REDISCOVER SERVICES
     */
    private fun refreshServices() {
        if (bluetoothGatt != null && bluetoothGatt?.device != null) {
            refreshDeviceCache()
            bluetoothGatt?.discoverServices()
        } else if (service != null && service?.connectedGatt != null) {
            refreshDeviceCache()
            service?.connectedGatt?.discoverServices()
        }
    }

    /**
     * CALLS A METHOD TO CLEAN DEVICE SERVICES
     */
    private fun refreshDeviceCache(): Boolean {
        try {
            Log.d("refreshDevice", "Called")
            val localMethod: Method = bluetoothGatt?.javaClass?.getMethod("refresh")!!
            val bool: Boolean = (localMethod.invoke(bluetoothGatt, *arrayOfNulls(0)) as Boolean)
            Log.d("refreshDevice", "bool: $bool")
            return bool
        } catch (localException: Exception) {
            Log.e("refreshDevice", "An exception occured while refreshing device")
        }
        return false
    }

    /**
     * CLEANS USER INTERFACE AND FINISH ACTIVITY
     */
    fun exit(gatt: BluetoothGatt?) {
        gatt?.close()
        service?.connectedGatt?.close()
        service?.clearCache()
        disconnect_gatt = false

        handler.postDelayed({
            bluetoothGatt = null
            service = null
            if (loadingdialog != null && loadingdialog?.isShowing!!) loadingdialog?.dismiss()
//            if (otaProgress != null && otaProgress?.isShowing!!) otaProgress?.dismiss()
//            if (otaSetup != null && otaSetup?.isShowing!!) otaSetup?.dismiss()
            finish()
        }, 1000)
    }

    /**
     * DISCONNECTS AND CONNECTS WITH THE SELECTED DELAY
     */
    fun reconnect(delaytoconnect: Long) {
        val reconnectTimer = Timer()
        bluetoothDevice = bluetoothGatt?.device
        if (service?.isGattConnected!!) {
            service?.clearGatt()
            service?.clearCache()
        }
        bluetoothGatt?.disconnect()
        reconnectTimer.schedule(object : TimerTask() {
            override fun run() {
                bluetoothGatt?.close()
            }
        }, 400)
        reconnectTimer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (loadingdialog?.isShowing!!) {
                        loadingLog?.text = "Attempting connection..."
                    }
                }
                bluetoothGatt = bluetoothDevice?.connectGatt(
                    applicationContext,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            }
        }, delaytoconnect)
    }

    /**
     * OTA STATE MACHINE
     */
    @Synchronized
    fun dfuMode(step: String?) {

        when (step) {
            "INIT" -> dfuMode("OTABEGIN")
            "OTABEGIN" -> if (ota_mode) {
                //START OTA PROCESS -> gattCallback -> OnCharacteristicWrite
                Log.d("OTA_BEGIN", "true")
                handler.postDelayed(WRITE_OTA_CONTROL_ZERO, 200)
            } else {
                if (homekit) {
                    bluetoothGatt?.readDescriptor(kit_descriptor)
                } else {
                    Log.d("DFU_MODE", "true")
                    handler.postDelayed(WRITE_OTA_CONTROL_ZERO, 200)
                }
            }
            "OTAUPLOAD" -> {
                Log.d("OTAUPLOAD", "Called")
                /**Check Services */
                val mBluetoothGattService = bluetoothGatt?.getService(ota_service)
                if (mBluetoothGattService != null) {
                    val charac =
                        bluetoothGatt?.getService(ota_service)!!.getCharacteristic(ota_data)
                    if (charac != null) {
                        charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        Log.d("Instance ID", "" + charac.instanceId)
                        /**Check Files */
                        var ebl: ByteArray? = null
                        try {
                            Log.d("stackPath", "" + stackPath)
                            Log.d("appPath", "" + applicationPath)
                            val file: File
                            if (stackPath != "" && doubleStepUpload) {
                                file = File(stackPath)
                                boolFullOTA = true
                            } else {
                                file = File(applicationPath)
                                boolFullOTA = false
                            }
                            val fileInputStream = FileInputStream(file)
                            val size = fileInputStream.available()
                            Log.d("size", "" + size)
                            val temp = ByteArray(size)
                            fileInputStream.read(temp)
                            fileInputStream.close()
                            ebl = temp
                        } catch (e: Exception) {
                            Log.e("InputStream", "Couldn't open file$e")
                        }
                        val datathread = ebl
                        otafile = ebl
                        /**Check if it is partial of full OTA */
                        val fn: String
                        if (stackPath != "" && doubleStepUpload) {
                            val last = stackPath.lastIndexOf(File.separator)
                            fn = stackPath.substring(last)
                            Log.d("CurrentlyUpdating", "apploader")
                        } else {
                            val last = applicationPath.lastIndexOf(File.separator)
                            fn = applicationPath.substring(last)
                            Log.d("CurrentlyUpdating", "appliaction")
                        }
                        pack = 0
                        /**Prepare information about current upload step */
                        val stepInfo: String
                        if (doubleStepUpload) {
                            if (stackPath != "") {
                                stepInfo = "1 OF 2"
                            } else {
                                stepInfo = "2 OF 2"
                            }
                        } else {
                            stepInfo = "1 OF 1"
                        }
                        /**Set info into UI OTA Progress */
//                        runOnUiThread {
//                            filename?.text = fn
//                            steps?.text = stepInfo
//                            sizename?.text = datathread?.size.toString() + " bytes"
//                            mtuname?.text = MTU.toString()
//                            uploadimage?.visibility = View.VISIBLE
//                            animaloading()
//                        }
                        /**Start OTA_data Upload in another thread */
                        val otaUpload = Thread(Runnable {
                            if (isReliable) {
                                otaWriteDataReliable()
                            } else writeOtaData(datathread)
                        })
                        otaUpload.start()
                    }
                }
            }
            "OTAEND" -> {
                Log.d("OTAEND", "Called")
                handler.postDelayed({ writeOtaControl(0x03.toByte()) }, 500)
            }
            "DISCONNECTION" -> {
                //   ota_process = false
                boolFullOTA = false
                //    boolOTAbegin = false
                disconnectGatt(bluetoothGatt)
            }
            else -> {
            }
        }
    }

    /**
     * WRITES OTA CONTROL FOR HOMEKIT DEVICES
     */
    fun homeKitOTAControl(instanceID: ByteArray) {

        //WRITE CHARACTERISTIC FOR HOMEKIT
        val value = byteArrayOf(
            0x00,
            0x02,
            0xee.toByte(),
            instanceID[0],
            instanceID[1],
            0x03,
            0x00,
            0x01,
            0x01,
            0x01
        )
        writeGenericCharacteristic(ota_service, ota_control, value)
    }

    /**
     * WRITES BYTE ARRAY TO A GENERIC CHARACTERISTIC
     */
    private fun writeGenericCharacteristic(
        service: UUID?,
        characteristic: UUID?,
        value: ByteArray?
    ): Boolean {
        if (bluetoothGatt != null) {
            val bluetoothGattCharacteristic =
                bluetoothGatt?.getService(service)?.getCharacteristic(characteristic)
            Log.d("characteristic", "exists")
            if (bluetoothGattCharacteristic != null) {
                bluetoothGattCharacteristic.value = value
                bluetoothGattCharacteristic.writeType =
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                bluetoothGatt?.writeCharacteristic(bluetoothGattCharacteristic)
                Log.d("characteristic", "written")
            } else {
                Log.d("characteristic", "null")
                return false
            }
        } else {
            Log.d("bluetoothGatt", "null")
            return false
        }
        return true
    }

    /**
     * WRITES EBL/GBL FILES TO OTA_DATA CHARACTERISTIC
     */
    @Synchronized
    fun otaWriteDataReliable() {
        Log.d("Test", "otaWriteDataReliable")
        boolOTAdata = true
        if (pack == 0) {
            /**SET MTU_divisible by 4 */
            var minus = 0
            do {
                mtuDivisible = currentMTU - 3 - minus
                minus++
            } while (mtuDivisible % 4 != 0)
            //   runOnUiThread { mtuname?.text = "$mtuDivisible bytes" }
        }
        val writearray: ByteArray
        val pgss: Float
        if (pack + mtuDivisible > otafile?.size!! - 1) {
            /**SET last by 4 */
            var plus = 0
            var last = otafile?.size!! - pack
            do {
                last += plus
                plus++
            } while (last % 4 != 0)
            writearray = ByteArray(last)
            for ((j, i) in (pack until pack + last).withIndex()) {
                if (otafile?.size!! - 1 < i) {
                    writearray[j] = 0xFF.toByte()
                } else writearray[j] = otafile!![i]
            }
            pgss = ((pack + last).toFloat() / (otafile?.size!! - 1)) * 100
        } else {
            var j = 0
            writearray = ByteArray(mtuDivisible)
            for (i in pack until pack + mtuDivisible) {
                writearray[j] = otafile!![i]
                j++
            }
            pgss = ((pack + mtuDivisible).toFloat() / (otafile?.size!! - 1)) * 100
        }
        val charac = bluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_data)
        charac?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        charac?.value = writearray
        bluetoothGatt?.writeCharacteristic(charac)
        val waiting_time = (System.currentTimeMillis() - otatime)
        val bitrate = 8 * pack.toFloat() / waiting_time
        if (pack > 0) {
            handler.post {
//                runOnUiThread {
//                    progressBar?.progress = pgss.toInt()
//                    val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
//                    dataRate?.text = datarate
//                    datasize?.text = pgss.toInt().toString() + " %"
//                }
            }
        } else {
            otatime = System.currentTimeMillis()
        }
    }

    @Synchronized
    fun writeOtaData(datathread: ByteArray?) {
        try {
            boolOTAdata = true
            val value = ByteArray(currentMTU - 3)
            val start = System.nanoTime()
            var j = 0
            for (i in datathread?.indices!!) {
                value[j] = datathread[i]
                j++
                if (j >= currentMTU - 3 || i >= (datathread.size - 1)) {
                    var wait = System.nanoTime()
                    val charac = bluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_data)
                    charac?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    val progress = ((i + 1).toFloat() / datathread.size) * 100
                    val bitrate =
                        (((i + 1) * (8.0)).toFloat() / (((wait - start) / 1000000.0).toFloat()))
                    if (j < currentMTU - 3) {
                        val end = ByteArray(j)
                        System.arraycopy(value, 0, end, 0, j)
                        runOnUiThread {
//                            datasize?.text = progress.toInt().toString() + " %"
//                            progressBar?.progress = progress.toInt()
                        }
                        charac?.value = end
                    } else {
                        j = 0
                        runOnUiThread {
//                            datasize?.text = progress.toInt().toString() + " %"
//                            progressBar?.progress = progress.toInt()
                        }
                        charac?.value = value
                    }
                    if (bluetoothGatt?.writeCharacteristic(charac)!!) {
                        runOnUiThread {
                            val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
                            // dataRate?.text = datarate
                        }
                        while ((System.nanoTime() - wait) / 1000000.0 < delayNoResponse);
                    } else {
                        do {
                            while ((System.nanoTime() - wait) / 1000000.0 < delayNoResponse);
                            wait = System.nanoTime()
                            runOnUiThread {
                                val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
                                //   dataRate?.text = datarate
                            }
                        } while (!bluetoothGatt?.writeCharacteristic(charac)!!)
                    }
                }
            }
            val end = System.currentTimeMillis()
            val time = (end - start) / 1000L.toFloat()
            Log.d("OTA Time - ", "" + time + "s")
            boolOTAdata = false
            runOnUiThread {
                chrono?.stop()
//                uploadimage?.clearAnimation()
//                uploadimage?.visibility = View.INVISIBLE
            }
            dfuMode("OTAEND")
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

}