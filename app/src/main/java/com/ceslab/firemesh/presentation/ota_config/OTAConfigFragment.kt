package com.ceslab.firemesh.presentation.ota_config

import android.app.Activity
import android.app.Dialog
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Handler
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import com.ceslab.firemesh.R
import com.ceslab.firemesh.ota.callbacks.TimeoutGattCallback
import com.ceslab.firemesh.ota.model.DFUStep
import com.ceslab.firemesh.ota.model.OTAConverters
import com.ceslab.firemesh.ota.model.OTAFile
import com.ceslab.firemesh.ota.model.OTAType
import com.ceslab.firemesh.ota.service.OTAService
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.ota_config.dialog.ErrorDialog
import kotlinx.android.synthetic.main.fragment_ota_config.*
import timber.log.Timber
import java.io.*
import java.lang.reflect.Method
import java.util.*

/**
 * Created by Viet Hua on 04/22/2021.
 */


class OTAConfigFragment : BaseFragment(){
    companion object {
        const val TAG = "OTAConfigFragment"
        private const val FILE_CHOOSER_REQUEST_CODE = 9999
        private const val UI_CREATION_DELAY = 0
        private const val RECONNECTION_RETRIES = 3
        private const val GATT_FETCH_ON_SERVICE_DISCOVERED_DELAY = 875

        var ota_service = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0")
        private val ota_control = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063")
        private val ota_data = UUID.fromString("984227f3-34fc-4045-a5d0-2c581f81a153")
        private val homekit_descriptor = UUID.fromString("dc46f0fe-81d2-4616-b5d9-6abdd796939a")
        private val homekit_service = UUID.fromString("0000003e-0000-1000-8000-0026bb765291")

    }

    private var disconnect_gatt = false
    private var kit_descriptor: BluetoothGattDescriptor? = null
    private var doubleStepUpload = false
    private var otafile: ByteArray? = null
    private var pack = 0
    private var boolOTAdata = false
    private var delayNoResponse = 1
    private var otaProcessing = false
    private var connected = false
    private var discoverTimeout = true
    private var UICreated = false

    private var retryAttempts = 0
    private val DFU_OTA_UPLOAD = Runnable { dfuMode(DFUStep.UPLOAD) }
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
    private var isInsideOTAMode = false
    private var boolFullOTA = false
    private var boolOTAFirstStep = false
    private var mtuDivisible = 0
    private var otatime: Long = 0


    // OTA file paths
    private var currentOtaFileType: OTAFile? = null

    private var applicationPath = ""
    private var appLoaderPath = ""
    private var currentMTU = 247
    private var currentPriority = 1
    private var isReliable = true




    override fun getResLayoutId(): Int {
        return R.layout.fragment_ota_config
    }

    override fun onMyViewCreated(view: View) {
       Timber.d("onMyViewCreated")
        handler = Handler()
        showProgressDialog("Initializing...")
        setupViews()
        initLoading()
        initDevice("")

    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        boolOTAFirstStep = false
        otaProcessing = false
        loadingdialog?.dismiss()
        bluetoothBinding?.unbind()
        bluetoothGatt?.let {
            disconnectGatt(bluetoothGatt)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                FILE_CHOOSER_REQUEST_CODE -> {
                    val type = currentOtaFileType
                    val uri = data?.data
                    val filename: String?

                    filename = try {
                        getFileName(uri)
                    } catch (e: Exception) {
                        ""
                    }

                    if (!hasOtaFileCorrectExtension(filename)) {
                        showToastMessage("Incorrect File!")
                        return
                    }

                    if(type == OTAFile.APPLICATION){
                        prepareOtaFile(OTAFile.APPLICATION,uri, filename)
                    } else {
                        prepareOtaFile(OTAFile.APPLOADER,uri, filename)

                    }
                }
            }
        }

        if(areFullOTAFilesCorrect() && doubleStepUpload) {
            btn_ota_proceed.isClickable = true
            btn_ota_proceed.setBackgroundColor(ContextCompat.getColor(context!!, R.color.primary_color))

        } else if (arePartialOTAFilesCorrect() && !doubleStepUpload) {
            btn_ota_proceed.isClickable = true
            btn_ota_proceed.setBackgroundColor(ContextCompat.getColor(context!!, R.color.primary_color))
        } else {
            btn_ota_proceed.isClickable = false
            btn_ota_proceed.setBackgroundColor(ContextCompat.getColor(context!!, R.color.gray_7))
        }
    }

    /**
     * INITIALIZES LOADING DIALOG
     */
    private fun initLoading() {
        Timber.d("initLoading")
        loadingdialog = Dialog(activity as Context)
        loadingdialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        loadingdialog?.setContentView(R.layout.dialog_ota_loading)

        loadingimage = loadingdialog?.findViewById(R.id.connecting_spinner)
        loadingLog = loadingdialog?.findViewById(R.id.loadingLog)
        loadingHeader = loadingdialog?.findViewById(R.id.loading_header)
    }

    private fun setupViews() {
        Timber.d("setupViews")
        btn_ota_proceed.apply {
            setBackgroundColor(ContextCompat.getColor(this@OTAConfigFragment.context!!, R.color.gray_7))
            setOnClickListener(onProceedButtonClicked)
        }

        btn_ota_end.apply {
            setBackgroundColor(ContextCompat.getColor(this@OTAConfigFragment.context!!, R.color.gray_7))
            setOnClickListener(onEndOTAButtonClicked)
        }


        btn_select_application_gbl_file.setOnClickListener(onSelectApplicationFileClicked)
        btn_select_apploader_gbl_file.setOnClickListener(onSelectApploaderFileClicked)

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

                if (arePartialOTAFilesCorrect()) {
                    btn_ota_proceed?.isClickable = true
                    btn_ota_proceed?.setBackgroundColor(ContextCompat.getColor(this@OTAConfigFragment.context!!, R.color.primary_color))
                } else {
                    btn_ota_proceed?.isClickable = false
                    btn_ota_proceed?.setBackgroundColor(ContextCompat.getColor(this@OTAConfigFragment.context!!, R.color.gray_7))
                }
            }

            OTAType.FULL_OTA -> {
                btn_full_ota.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.button_clicked_color))
                btn_partial_ota.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.primary_color))
                layout_app_loader.visibility = View.VISIBLE

                if (areFullOTAFilesCorrect()) {
                    btn_ota_proceed?.isClickable = true
                    btn_ota_proceed?.setBackgroundColor(ContextCompat.getColor(this@OTAConfigFragment.context!!, R.color.primary_color))
                } else {
                    btn_ota_proceed?.isClickable = false
                    btn_ota_proceed?.setBackgroundColor(ContextCompat.getColor(this@OTAConfigFragment.context!!, R.color.gray_7))
                }
            }
        }
    }


    private fun areFullOTAFilesCorrect(): Boolean {
        return btn_select_application_gbl_file.text != "Select Application .gbl file" && btn_select_apploader_gbl_file.text != "Select Apploader .gbl file"
    }

    private fun arePartialOTAFilesCorrect(): Boolean {
        Timber.d("arePartialOTAFilesCorrect")
        return btn_select_application_gbl_file.text != "Select Application .gbl file"
    }

    private fun getFileName(uri: Uri?): String? {
        Timber.d("getFileName")
        var result: String? = null
        if ((uri?.scheme == "content")) {
            val cursor =activity!!.contentResolver.query(uri, null, null, null, null)
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

    private fun hasOtaFileCorrectExtension(filename: String?): Boolean {
        Timber.d("hasOtaFileCorrectExtension")
        return filename?.toUpperCase(Locale.getDefault())?.contains(".GBL")!!
    }

    private fun prepareOtaFile(type:OTAFile, uri: Uri?, fileName: String?) {
        Timber.d("prepareOtaFile")
        try {
            val inStream = activity!!.contentResolver.openInputStream(uri!!) ?: return
            val file = File(activity?.cacheDir, fileName)
            val output: OutputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while ((inStream.read(buffer).also { read = it }) != -1) {
                output.write(buffer, 0, read)
            }

            if(type == OTAFile.APPLICATION) {
                applicationPath = file.absolutePath
                btn_select_application_gbl_file.text = fileName
            } else {
                appLoaderPath = file.absolutePath
                btn_select_apploader_gbl_file.text = fileName

            }
            output.flush()
        } catch (e: IOException) {
            e.printStackTrace()
            showToastMessage("Incorrect File!")
        }
    }


    private val onProceedButtonClicked = View.OnClickListener {
        Timber.d("onProceedButtonClicked: isReliable=$isReliable -- mtu=$currentMTU -- priority=$currentPriority")
        btn_ota_proceed.isClickable = false
        btn_ota_proceed.setBackgroundColor(ContextCompat.getColor(this@OTAConfigFragment.context!!, R.color.gray_7))
        btn_ota_end.isClickable = true
        btn_ota_end.setBackgroundColor(ContextCompat.getColor(this@OTAConfigFragment.context!!, R.color.red_500))

        otaProcessing = true

        if (isInsideOTAMode) {
            Timber.d("isInsideOTAMode = true")
            bluetoothGatt?.requestMtu(edt_mtu_value?.text.toString().toInt())
        } else  {
            Timber.d("isInsideOTAMode = false")
            dfuMode(DFUStep.BEGIN)
        }
    }

    private val onEndOTAButtonClicked = View.OnClickListener {
        Timber.d("onEndOTAButtonClicked")
        dfuMode(DFUStep.DISCONNECTION)
    }


    private val onSelectApplicationFileClicked = View.OnClickListener {
        Timber.d("onSelectApplicationFileClicked")
        val intent = Intent().apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
        }
        currentOtaFileType = OTAFile.APPLICATION
        startActivityForResult(
            Intent.createChooser(intent, "Choose directory"), FILE_CHOOSER_REQUEST_CODE)
    }

    private val onSelectApploaderFileClicked = View.OnClickListener {
        Timber.d("onSelectApploaderFileClicked")
        val intent = Intent().apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
        }
        currentOtaFileType = OTAFile.APPLOADER

        startActivityForResult(
            Intent.createChooser(intent, "Choose directory"), FILE_CHOOSER_REQUEST_CODE)
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
        Timber.d("initDevice: $deviceAddress")
        bluetoothBinding = object : OTAService.Binding(context!!) {
            override fun onBound(service: OTAService?) {
                serviceHasBeenSet = true
                this@OTAConfigFragment.service = service
                if (!service?.isGattConnected(deviceAddress)!!) {
                    disconnectGatt(bluetoothGatt)
                } else {
                    val bG = service.getConnectedGatt(deviceAddress)
                    if (bG == null) {
                        activity!!.onBackPressed()
                        return
                    }

                    service.registerGattCallback(false, gattCallback)
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
            { activity?.runOnUiThread { bluetoothBinding?.bind() } },
            UI_CREATION_DELAY.toLong()
        )
    }

    /**
     * SETS ALL THE INFO IN THE OTA PROGRESS DIALOG TO "" OR 0
     */
    fun resetOTAProgress() {
        Timber.d("resetOTAProgress")
        boolFullOTA = false
       activity?.runOnUiThread {
            tv_data_size.text = ""
            tv_file_name.text = ""
            loadingimage?.visibility = View.GONE
            loadingdialog?.dismiss()
            progress_bar_ota_progress.progress = 0
            tv_data_size?.text = "0%"
            tv_data_rate?.text = ""
            btn_ota_end.isClickable = false
            btn_ota_end.setBackgroundColor(ContextCompat.getColor(this@OTAConfigFragment.context!!, R.color.gray_7))
        }
        dfuMode(DFUStep.BEGIN) //OTAProgress

    }


    /**
     * INITILIAZES ALL NECESSARY DIALOGS AND VIEW IN UI - ONCREATE
     */
    private fun onGattFetched() {
        Timber.d("onGattFetched: isInsideOTAMode = $isInsideOTAMode - otaFirstStep=$boolOTAFirstStep")
        var deviceName = bluetoothGatt?.device?.name
        if(deviceName == null) {
            tv_node_name?.let {
                it.text = "Unknown Device"
            }
            (activity as MainActivity).supportActionBar?.title = "Unknown Device"
        } else {
            tv_node_name?.let {
                it.text = deviceName
            }
            (activity as MainActivity).supportActionBar?.title = deviceName


        }
        hideDialog()
        UICreated = true

        if (isInsideOTAMode) {
            otaProcessing = true
            boolOTAFirstStep = false
        } else {
            otaProcessing = true
            boolOTAFirstStep = true
        }


    }



    fun disconnectGatt(gatt: BluetoothGatt?) {
        Timber.d("disconnectGatt")
        boolFullOTA = false
        boolOTAFirstStep = false
        otaProcessing = false
        disconnect_gatt = true
        UICreated = false
        val disconnectTimer = Timer()

        if (gatt != null && gatt.device != null) {
            if (loadingdialog == null) {
                initLoading()
            }
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
                        activity?.onBackPressed()
                    }
                }, 5000)
            }
            Thread(timeout).start()
        } else {
            activity?.onBackPressed()
        }
    }

    /**
     * WRITES BYTE TO OTA CONTROL CHARACTERISTIC
     */
    private fun writeOtaControl(ctrl: Byte): Boolean {
        Timber.d("writeOtaControl: $ctrl")
        if (bluetoothGatt?.getService(ota_service) != null) {
            val charac = bluetoothGatt?.getService(ota_service)?.getCharacteristic(
                ota_control
            )
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

    /**
     * (RUNNABLE) CHECKS OTA BEGIN BOX AND STARTS
     */
    private val checkbeginrunnable: Runnable = Runnable {
        chrono?.base = SystemClock.elapsedRealtime()
        chrono?.start()
    }


    private val gattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {}
        override fun onTimeout() {
            super.onTimeout()
            Timber.d("onTimeout")
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Timber.d("onMtuChanged: $mtu - status: $status")

            if (status == 0) { //NO ERRORS
                currentMTU = mtu
                bluetoothGatt?.requestConnectionPriority(currentPriority)
                if (otaProcessing) {
                    if (isInsideOTAMode) { //Reset OTA Progress
                        resetOTAProgress()
                    }
                }
            } else { //ERROR HANDLING
                Timber.d("onMtuChanged error: $status")
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
            Timber.d("onConnectionStateChange: status=$status - newState=$newState")
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    connected = true
                    Timber.d("onConnectionStateChange: CONNECTED")
                   activity?.runOnUiThread {
                        if (!loadingdialog?.isShowing!!) {
                            showToastMessage("DEVICE CONNECTED")
                        }
                    }
                    if (otaProcessing) { //After OTA process started
                        Timber.d("onConnectionStateChange: address=${gatt.device} - name=${gatt.device.name}")
                        if (gatt.services.isEmpty()) {
                            handler.postDelayed({
                                bluetoothGatt = null //It's going to be equal gatt in Discover Services Callback...
                                Timber.d("Start Service Discovery = ${gatt.discoverServices()}")
                            }, 250)
                            discoverTimeout = true
                            val timeout = Runnable {
                                handler.postDelayed({
                                    if (discoverTimeout) {
                                        disconnectGatt(gatt)
                                        activity?.runOnUiThread { showToastMessage("DISCOVER SERVICES TIMEOUT") }
                                    }
                                }, 25000)
                            }
                            Thread(timeout).start()
                        }
                    }
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    Timber.d("onConnectionStateChange: STATE_DISCONNECTED")
                    if (status == 133 && retryAttempts < RECONNECTION_RETRIES) {
                        retryAttempts++
                        Timber.d("onConnectionStateChange: Reconnect due to 0x85 (133) error")
                        reconnect(1000)
                        return
                    }
                    connected = false
                    discoverTimeout = false
                    disconnectionTimeout = false
                    if ((status != 0)  && (errorDialog == null)) {
                        Timber.d("errorDialog null - failed")
                        activity?.runOnUiThread {
                            errorDialog = ErrorDialog(status, object : ErrorDialog.OtaErrorCallback {
                                override fun onDismiss() {
                                    exit(bluetoothGatt)
                                }
                            })
                            errorDialog?.show(fragmentManager!!, "ota_error_dialog")
                        }
                    } else {
                        if (disconnect_gatt) {
                            exit(gatt)
                        }
                        if (otaProcessing || boolOTAFirstStep || boolFullOTA) {
                          activity?.runOnUiThread {
                                if (loadingdialog?.isShowing!!) {
                                    loadingLog?.text = "Rebooting..."
                                    handler.postDelayed({ activity?.runOnUiThread { loadingLog?.text = "Waiting..." } }, 1500)
                                }
                            }
                        }
//                        if (otaSetup != null) if (otaSetup?.isShowing!!) {
//                            exit(gatt)
//                        }
                        if (gatt.services.isEmpty()) {
                            exit(gatt)
                        }
                        if (!boolFullOTA && !boolOTAFirstStep && !otaProcessing) {
                            exit(gatt)
                        }
                    }
                }
                BluetoothGatt.STATE_CONNECTING -> Timber.d("onConnectionStateChange: Connecting...")
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
                    Timber.d("homekit_descriptor: Insecure Connection")
                   activity?.runOnUiThread { showToastMessage("Error: Not a Homekit Secure Connection") }
                } else if (value[2] == 0x04.toByte()) {
                    Timber.d("homekit_descriptor: Wrong Address")
                } else if (value[2] == 0x00.toByte()) {
                    Timber.d("homekit_descriptor: Entering in DFU_Mode")
                    if (isInsideOTAMode && otaProcessing) {
                        Timber.d("OTA_UPLOAD: Sent")
                      activity?.runOnUiThread(checkbeginrunnable)
                        handler.removeCallbacks(DFU_OTA_UPLOAD)
                        handler.postDelayed(DFU_OTA_UPLOAD, 500)
                    } else if (!isInsideOTAMode && otaProcessing) {
                      activity?.runOnUiThread {
                            loadingLog?.text = "Resetting..."
                            loadingdialog?.let { dialog ->
                                dialog.show()
                                dialog.setCanceledOnTouchOutside(false)
                            }
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
            Timber.d("onCharacteristicWrite: status=$status -- isInsideOTAMode=$isInsideOTAMode --otaProcessing=$otaProcessing")
            if (characteristic.value.size < 10) Timber.d( "onCharacteristicWrite: Char: " + characteristic.uuid.toString() + " Value: " + OTAConverters.bytesToHexWhitespaceDelimited(characteristic.value) + " Status: " + status)
            if (status != 0) { // Error Handling
                if (errorDialog == null) {
                   activity?.runOnUiThread {
                        errorDialog = ErrorDialog(status, object : ErrorDialog.OtaErrorCallback {
                            override fun onDismiss() {
                                exit(bluetoothGatt)
                            }
                        })
                        errorDialog?.show(fragmentManager!!, "ota_error_dialog")
                    }
                }
            } else {
                if ((characteristic.uuid == ota_control)) { //OTA Control Callback Handling
                    Timber.d("OTA Control Callback Handling")
                    if (characteristic.value.size == 1) {
                        if (characteristic.value[0] == 0x00.toByte()) {
                            if (isInsideOTAMode && otaProcessing) {
                                Timber.d("OTAUPLOAD: send")
                                activity?.runOnUiThread(checkbeginrunnable)
                                handler.removeCallbacks(DFU_OTA_UPLOAD)
                                handler.postDelayed(DFU_OTA_UPLOAD, 500)
                            } else if (!isInsideOTAMode && otaProcessing) {
                                activity?.runOnUiThread {
                                    loadingLog?.text = "Resetting..."
                                    loadingdialog?.let { dialog ->
                                        dialog.show()
                                        dialog.setCanceledOnTouchOutside(false)
                                    }
                                }
                                handler.post { reconnect(4000) }
                            }
                        }
                        if (characteristic.value[0] == 0x03.toByte()) {
                            if (otaProcessing) {
                                activity?.runOnUiThread {
                                    btn_ota_end.setBackgroundColor(ContextCompat.getColor(this@OTAConfigFragment.context!!, R.color.red_500))
                                    btn_ota_end.isClickable = true
                                }
                                boolOTAFirstStep = false
                                if (boolFullOTA) {
                                    appLoaderPath = ""
                                    activity?.runOnUiThread {
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
                            Timber.d("Homekit: Reading OTA Control")
                            bluetoothGatt?.readCharacteristic(characteristic)
                        }
                    }
                }
                if ((characteristic.uuid == ota_data)) {   //OTA Data Callback Handling
                    Timber.d("OTA Data Callback Handling: isReliable=$isReliable")
                    if (isReliable) {
                        pack += mtuDivisible
                        if (pack <= otafile?.size!! - 1) {
                            otaWriteDataReliable()
                        } else if (pack > otafile?.size!! - 1) {
                            handler.post {
                                activity?.runOnUiThread {
                                    chrono?.stop()
                                    spinner_connecting.clearAnimation()
                                    spinner_connecting.visibility = View.INVISIBLE
                                }
                            }
                            boolOTAdata = false
                            retryAttempts = 0
                            dfuMode(DFUStep.END)
                        }
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
            activity?.runOnUiThread {
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
                activity?.runOnUiThread {
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
            Timber.d("onServicesDiscovered: $status")
            super.onServicesDiscovered(gatt, status)
            if (bluetoothGatt != gatt) {
                bluetoothGatt = gatt
                refreshServices()
            } else {
                discoverTimeout = false
                /**ERROR IN SERVICE DISCOVERY */
                if (status != 0) {
                    Timber.e("onServicesDiscovered: Error status: $status")
                    if (errorDialog == null) {
                        activity?.runOnUiThread {
                            errorDialog = ErrorDialog(status, object : ErrorDialog.OtaErrorCallback {
                                override fun onDismiss() {
                                    exit(bluetoothGatt)
                                }
                            })
                            errorDialog?.show(fragmentManager!!, "ota_error_dialog")
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
                                isInsideOTAMode = true
                                val otaDataProperty = gatt.getService(ota_service)
                                    .getCharacteristic(ota_data).properties
                                if ((otaDataProperty == 12) || (otaDataProperty == 8) || (otaDataProperty == 10)) {
                                    //reliable = true;
                                } else if (isInsideOTAMode && otaDataProperty == 4) {
                                    //reliable = false;
                                }
                            }
                        } else {
                            if (boolOTAFirstStep) onceAgain()
                        }
                    }

                    //REQUEST MTU && loadingdialog?.isShowing!!
                    if (UICreated) {
                        bluetoothGatt?.requestMtu(currentMTU)
                    }

                    //LAUNCH SERVICES UI
                    if (!boolFullOTA) {
                        handler.postDelayed({
                            activity?.runOnUiThread {
                                onGattFetched()
                            }
                        }, GATT_FETCH_ON_SERVICE_DISCOVERED_DELAY.toLong())
                    }

                    //IF DFU_MODE, LAUNCH OTA SETUP AUTOMATICALLY
                    if (isInsideOTAMode && boolOTAFirstStep) {
                        handler.postDelayed({
                            activity?.runOnUiThread {
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
        Timber.d("getServiceInfo: Service count: ${gattServices.size}")
        for (gattService: BluetoothGattService in gattServices) {
            val serviceUUID = gattService.uuid.toString()
            Timber.d("getServiceInfo: Service UUID : $serviceUUID -- Char count: ${gattService.characteristics.size}")
            val gattCharacteristics = gattService.characteristics
            for (gattCharacteristic: BluetoothGattCharacteristic in gattCharacteristics) {
                val characteristicUUID = gattCharacteristic.uuid.toString()
                Timber.d("getServiceInfo: Characteristic UUID: $characteristicUUID - Props: ${gattCharacteristic.properties}")
                if ((gattCharacteristic.uuid.toString() == ota_control.toString())) {
                    if (gattCharacteristics.contains(
                            bluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_data
                            )
                        )
                    ) {
                        if (!gattServices.contains(bluetoothGatt?.getService(homekit_service))) {
                            Timber.d("getServiceInfo: Device in DFU Mode")
                        } else {
                            Timber.d("getServiceInfo: OTA_Control_found")
                            val gattDescriptors = gattCharacteristic.descriptors
                            for (gattDescriptor: BluetoothGattDescriptor in gattDescriptors) {
                                val descriptor = gattDescriptor.uuid.toString()
                                if ((gattDescriptor.uuid.toString() == homekit_descriptor.toString())) {
                                    kit_descriptor = gattDescriptor
                                    Timber.d("descriptor: UUID: $descriptor")
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
            Timber.d("refreshDeviceCache: Called")
            val localMethod: Method = bluetoothGatt?.javaClass?.getMethod("refresh")!!
            val bool: Boolean = (localMethod.invoke(bluetoothGatt, *arrayOfNulls(0)) as Boolean)
            Timber.d("refreshDeviceCache: bool: $bool")
            return bool
        } catch (localException: Exception) {
            Timber.e("refreshDevice: An exception occured while refreshing device")
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
            activity?.onBackPressed()
        }, 1000)
    }

    /**
     * DISCONNECTS AND CONNECTS WITH THE SELECTED DELAY
     */
    fun reconnect(delaytoconnect: Long) {
        Timber.d("reconnect: $delaytoconnect")
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
                activity?.runOnUiThread {
                    if (loadingdialog?.isShowing!!) {
                        loadingLog?.text = "Attempting connection..."
                    }
                }
                bluetoothGatt = bluetoothDevice?.connectGatt(
                    context,
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
    fun dfuMode(step: DFUStep) {
        Timber.d("dfuMode: $step")
        when (step) {
            DFUStep.INIT -> dfuMode(DFUStep.BEGIN)
            DFUStep.BEGIN -> if (isInsideOTAMode) {
                //START OTA PROCESS -> gattCallback -> OnCharacteristicWrite
                Timber.d("dfuMode: isInsideOTAMode=$isInsideOTAMode")
                handler.postDelayed(WRITE_OTA_CONTROL_ZERO, 200)
            } else {
                Timber.d("dfuMode: isInsideOTAMode=$isInsideOTAMode -- homekit=$homekit")
                if (homekit) {
                    bluetoothGatt?.readDescriptor(kit_descriptor)
                } else {
                    handler.postDelayed(WRITE_OTA_CONTROL_ZERO, 200)
                }
            }
            DFUStep.UPLOAD -> {
                Timber.d("OTA_UPLOAD: called")
                /**Check Services */
                val mBluetoothGattService = bluetoothGatt?.getService(ota_service)
                if (mBluetoothGattService != null) {
                    val charac =
                        bluetoothGatt?.getService(ota_service)!!.getCharacteristic(ota_data
                        )
                    if (charac != null) {
                        charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        Timber.d("Instance ID : ${charac.instanceId}")
                        /**Check Files */
                        var ebl: ByteArray? = null
                        try {
                            Timber.d("stackPath $appLoaderPath")
                            Timber.d("appPath $applicationPath")

                            val file: File
                            if (appLoaderPath != "" && doubleStepUpload) {
                                file = File(appLoaderPath)
                                boolFullOTA = true
                            } else {
                                file = File(applicationPath)
                                boolFullOTA = false
                            }
                            val fileInputStream = FileInputStream(file)
                            val size = fileInputStream.available()
                            Timber.d("size: $size")
                            val temp = ByteArray(size)
                            fileInputStream.read(temp)
                            fileInputStream.close()
                            ebl = temp
                        } catch (e: Exception) {
                            Timber.e("InputStream: Couldn't open file $e")
                        }
                        val datathread = ebl
                        otafile = ebl
                        /**Check if it is partial of full OTA */
                        val fn: String
                        if (appLoaderPath != "" && doubleStepUpload) {
                            val last = appLoaderPath.lastIndexOf(File.separator)
                            fn = appLoaderPath.substring(last)
                            Timber.d("CurrentUpdating: apploader")
                        } else {
                            val last = applicationPath.lastIndexOf(File.separator)
                            fn = applicationPath.substring(last)
                            Timber.d("CurrentUpdating: application")
                        }
                        pack = 0
                        /**Prepare information about current upload step */
                        val stepInfo: String
                        if (doubleStepUpload) {
                            if (appLoaderPath != "") {
                                stepInfo = "1 OF 2"
                            } else {
                                stepInfo = "2 OF 2"
                            }
                        } else {
                            stepInfo = "1 OF 1"
                        }
                        /**Set info into UI OTA Progress */
                        activity?.runOnUiThread {
                            tv_file_name.text = fn
                            tv_ota_step.text = stepInfo
                            tv_file_size.text = datathread?.size.toString() + " bytes"
                            tv_packet_size.text = currentMTU.toString()
                            spinner_connecting.visibility = View.VISIBLE
                        }
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
            DFUStep.END -> {
                Timber.d("OTAEND: called")
                handler.postDelayed({ writeOtaControl(0x03.toByte()) }, 500)
            }
            DFUStep.DISCONNECTION -> {
                otaProcessing = false
                boolFullOTA = false
                boolOTAFirstStep  = false
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
            Timber.d("writeGenericCharacteristic: exists")
            if (bluetoothGattCharacteristic != null) {
                bluetoothGattCharacteristic.value = value
                bluetoothGattCharacteristic.writeType =
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                bluetoothGatt?.writeCharacteristic(bluetoothGattCharacteristic)
                Timber.d("writeGenericCharacteristic: written")
            } else {
                Timber.d("writeGenericCharacteristic: null")
                return false
            }
        } else {
            Timber.d("bluetoothGatt: null")
            return false
        }
        return true
    }

    /**
     * WRITES EBL/GBL FILES TO OTA_DATA CHARACTERISTIC
     */
    @Synchronized
    fun otaWriteDataReliable() {
        Timber.d("otaWriteDataReliable")
        boolOTAdata = true
        if (pack == 0) {
            /**SET MTU_divisible by 4 */
            var minus = 0
            do {
                mtuDivisible = currentMTU - 3 - minus
                minus++
            } while (mtuDivisible % 4 != 0)
            activity?.runOnUiThread { tv_packet_size.text = "$mtuDivisible bytes" }
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
            Timber.d( "otaWriteDataReliable: last: " + pack + " / " + (pack + last) + " : " + OTAConverters.bytesToHexWhitespaceDelimited(writearray))
        } else {
            var j = 0
            writearray = ByteArray(mtuDivisible)
            for (i in pack until pack + mtuDivisible) {
                writearray[j] = otafile!![i]
                j++
            }
            pgss = ((pack + mtuDivisible).toFloat() / (otafile?.size!! - 1)) * 100
            Timber.d( "otaWriteDataReliable: pack: " + pack + " / " + (pack + mtuDivisible) + " : " + OTAConverters.bytesToHexWhitespaceDelimited(writearray))

        }
        val charac = bluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_data
        )
        charac?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        charac?.value = writearray
        bluetoothGatt?.writeCharacteristic(charac)
        val waiting_time = (System.currentTimeMillis() - otatime)
        val bitrate = 8 * pack.toFloat() / waiting_time
        if (pack > 0) {
            handler.post {
                activity?.runOnUiThread {
                    progress_bar_ota_progress.progress = pgss.toInt()
                    val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
                    tv_data_rate.text = datarate
                    tv_data_size.text = pgss.toInt().toString() + " %"
                }
            }
        } else {
            otatime = System.currentTimeMillis()
        }
    }

    @Synchronized
    fun writeOtaData(datathread: ByteArray?) {
        Timber.d("writeOtaData")
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
                        Timber.d( "writeOtaData: sent " + (i + 1) + " / " + datathread.size + " - " + String.format("%.1f", progress) + " % - " + String.format("%.2fkbit/s", bitrate) + " - " + OTAConverters.bytesToHexWhitespaceDelimited(end))

                        activity?.runOnUiThread {
                            tv_data_size.text = progress.toInt().toString() + " %"
                            progress_bar_ota_progress.progress = progress.toInt()
                        }
                        charac?.value = end
                    } else {
                        j = 0
                        activity?.runOnUiThread {
                            tv_data_size.text = progress.toInt().toString() + " %"
                            progress_bar_ota_progress.progress = progress.toInt()
                        }
                        charac?.value = value
                    }
                    if (bluetoothGatt?.writeCharacteristic(charac)!!) {
                        activity?.runOnUiThread {
                            val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
                            Timber.d("datarate: $datarate")

                            tv_data_rate.text = datarate
                        }
                        while ((System.nanoTime() - wait) / 1000000.0 < delayNoResponse);
                    } else {
                        do {
                            while ((System.nanoTime() - wait) / 1000000.0 < delayNoResponse);
                            wait = System.nanoTime()
                            activity?.runOnUiThread {
                                val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
                                Timber.d("datarate: $datarate")
                                tv_data_rate.text = datarate
                            }
                        } while (!bluetoothGatt?.writeCharacteristic(charac)!!)
                    }
                }
            }
            val end = System.currentTimeMillis()
            val time = (end - start) / 1000L.toFloat()
            Timber.d("OTA Time: $time s")
            boolOTAdata = false
            activity?.runOnUiThread {
                chrono?.stop()
                spinner_connecting.clearAnimation()
                spinner_connecting.visibility = View.INVISIBLE
            }
            dfuMode(DFUStep.END)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }


}