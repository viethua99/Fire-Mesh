package com.ceslab.firemesh.meshmodule.ota

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import com.ceslab.firemesh.meshmodule.bluetoothle.BluetoothStateReceiver
import com.ceslab.firemesh.util.ConverterUtil
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class OTAManager(bluetoothStateReceiver: BluetoothStateReceiver) : ScanCallback(),
    BluetoothStateReceiver.BluetoothStateListener {

    companion object {
        //OTA Services
        private val OTA_SERVICE_UUID = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0")
        private val OTA_CONTROL_UUID = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063")
        private val OTA_DATA_UUID = UUID.fromString("984227f3-34fc-4045-a5d0-2c581f81a153")
        //HAP (Home Accessory Protocol) Services
        private val HOME_KIT_SERVICE_UUID = UUID.fromString("0000003e-0000-1000-8000-0026bb765291")
        private val HOME_KIT_DESCRIPTOR_UUID = UUID.fromString("dc46f0fe-81d2-4616-b5d9-6abdd796939a")
    }

    //Handlers
    private var handler: Handler = Handler()
    private val WRITE_OTA_CONTROL_ZERO = Runnable { writeOtaControl(0x00.toByte()) }
    private val DFU_OTA_UPLOAD = Runnable { dfuMode("OTA_UPLOAD") }


    private var otaFile: ByteArray? = null
    private var pack = 0
    private var mtuDivisible = 0
    private var otaTime: Long = 0
    private var delayNoResponse = 1
    private var isOtaMode = false
    private var isOtaBegin = false
    private var isOtaProcess = false
    private var isHomekit = false
    private var kitDescriptor: BluetoothGattDescriptor? = null
    private var doubleStepUpload = false // FOR FULL OTA

    // OTA file paths
    private var mAppPath = ""
    private var mAppLoaderPath = ""
    //OTA params
    private var mMtu = 247
    private var mPriority = 2
    private var mIsReliable = true

    //BLE Scan Instances
    private var leScanStarted: Boolean = false
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val scanCallbacks = ConcurrentLinkedQueue<ScanCallback>()
    private val otaConfigListeners: ArrayList<OTAConfigListener> = ArrayList()


    init {
        bluetoothStateReceiver.addListener(this)
    }

    fun addOtaConfigListener(otaConfigListener: OTAConfigListener) {
        otaConfigListeners.add(otaConfigListener)
    }

    fun addScanCallback(scanCallback: ScanCallback) {
        scanCallbacks.add(scanCallback)
    }

    fun removeScanCallback(scanCallback: ScanCallback) {
        scanCallbacks.remove(scanCallback)
    }

    fun startOTA(isReliable: Boolean, mtuValue: Int, priority: Int, appLoaderPath:String, appPath:String) {
        Timber.d(String.format("isReliable = $isReliable -- " +
                "mtuValue = $mtuValue -- " +
                "priority = $priority --" +
                "appLoaderPath = $appLoaderPath -- " +
                "appPath = $appPath"))
        if (isOtaMode) {
            isOtaProcess = true
            isOtaBegin = false
        } else {
            isOtaProcess = true
            isOtaBegin = true
        }

        mMtu = mtuValue
        mIsReliable = isReliable
        mPriority = priority
        mAppLoaderPath = appLoaderPath
        mAppPath = appPath
        //Start requesting mtu first
        bluetoothGatt?.requestMtu(mtuValue)
    }

    fun startLeScan(meshServ: ParcelUuid? = null): Boolean {
        synchronized(leScanStarted) {
            if (leScanStarted) {
                return true
            }
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
                return false
            }

            initBluetoothLeScanner()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            val filters = ArrayList<ScanFilter>()
            meshServ?.let {
                val filter = ScanFilter.Builder()
                    .setServiceUuid(meshServ)
                    .build()
                filters.add(filter)
            }
            bluetoothLeScanner?.apply {
                startScan(filters, settings, this@OTAManager)
                leScanStarted = true
                return true
            }
            return false
        }
    }

    fun stopLeScan() {
        synchronized(leScanStarted) {
            if (!leScanStarted) {
                return
            }
            bluetoothLeScanner?.apply {
                stopScan(this@OTAManager)
                leScanStarted = false
            }
        }
    }

    fun isLeScanStarted(): Boolean {
        return leScanStarted
    }

    fun connectDevice(context: Context, address: String) {
        Timber.d("connectDevice")
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Timber.e("BluetoothAdapter not initialized or unspecified address")
            return
        }

        val device = bluetoothAdapter!!.getRemoteDevice(address)

        if (device == null) {
            Timber.e("Device not found. Unable to connect.")
            return
        }
        bluetoothGatt = device.connectGatt(context, false, mGattCallback)
    }


    private fun initBluetoothLeScanner() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
    }


    @Synchronized
    private fun dfuMode(step: String?) {
        Timber.d("dfuMode : $step")
        when (step) {
            "OTA_BEGIN" -> {
                //START OTA PROCESS -> gattCallback -> OnCharacteristicWrite
                if(isOtaMode) {
                    Timber.d("OTA_BEGIN: true")
                    handler.postDelayed(WRITE_OTA_CONTROL_ZERO, 200)
                } else {
                    if(isHomekit) {
                        bluetoothGatt?.readDescriptor(kitDescriptor)
                    } else {
                        Timber.d( " DFU_MODE: true")
                        handler.postDelayed(WRITE_OTA_CONTROL_ZERO, 200)
                    }
                }
            }
            "OTA_UPLOAD" -> {
                /**Check Services */
                Timber.d( " OTA_UPLOAD: called")
                val mBluetoothGattService = bluetoothGatt?.getService(OTA_SERVICE_UUID)
                if (mBluetoothGattService != null) {
                    val charac = bluetoothGatt?.getService(OTA_SERVICE_UUID)!!.getCharacteristic(OTA_DATA_UUID)
                    if (charac != null) {
                        charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        /**Check Files */
                        var ebl: ByteArray? = null
                        try {
                            Timber.d("OTA_UPLOAD : appLoaderPath = $mAppLoaderPath")
                            Timber.d("OTA_UPLOAD : appPath = $mAppPath")
                            val file: File
                            file = File(mAppPath)
                            val fileInputStream = FileInputStream(file)
                            val size = fileInputStream.available()
                            Timber.d("OTA_UPLOAD : size = $size")
                            val temp = ByteArray(size)
                            fileInputStream.read(temp)
                            fileInputStream.close()
                            ebl = temp
                        } catch (e: Exception) {
                            Timber.e("OTA_UPLOAD :Couldn't open file $e")
                        }
                        val datathread = ebl
                        otaFile = ebl
                        /**Check if it is partial of full OTA */
                        val fn: String
                        if (mAppLoaderPath != "" && doubleStepUpload) {
                            val last = mAppLoaderPath.lastIndexOf(File.separator)
                            fn = mAppLoaderPath.substring(last)
                            Timber.d("OTA_UPLOAD: CurrentlyUpdating apploader ")
                        } else {
                            val last = mAppPath.lastIndexOf(File.separator)
                            fn = mAppPath.substring(last)
                            Timber.d("OTA_UPLOAD: CurrentlyUpdating appliaction ")
                        }
                        pack = 0
                        /**Prepare information about current upload step */

                        /**Start OTA_data Upload in another thread */
                        val otaUpload = Thread(Runnable {
                            if (mIsReliable) {
                                otaWriteDataReliable()
                            } else writeOtaData(datathread)
                        })
                        otaUpload.start()
                    }
                }
            }
            "OTA_END" -> {
                Log.d("OTAEND", "Called")
                handler.postDelayed({ writeOtaControl(0x03.toByte()) }, 500)
            }
            "DISCONNECTION" -> {
                isOtaProcess = false
               // boolFullOTA = false
                isOtaBegin = false
                bluetoothGatt?.disconnect()
            }
        }
    }



    @Synchronized
    fun otaWriteDataReliable() {
        if (pack == 0) {
            /**SET MTU_divisible by 4 */
            var minus = 0
            do {
                mtuDivisible = mMtu - 3 - minus
                minus++
            } while (mtuDivisible % 4 != 0)
        }
        val writearray: ByteArray
        val pgss: Float
        if (pack + mtuDivisible > otaFile?.size!! - 1) {
            /**SET last by 4 */
            var plus = 0
            var last = otaFile?.size!! - pack
            do {
                last += plus
                plus++
            } while (last % 4 != 0)
            writearray = ByteArray(last)
            for ((j, i) in (pack until pack + last).withIndex()) {
                if (otaFile?.size!! - 1 < i) {
                    writearray[j] = 0xFF.toByte()
                } else writearray[j] = otaFile!![i]
            }
            pgss = ((pack + last).toFloat() / (otaFile?.size!! - 1)) * 100
        } else {
            var j = 0
            writearray = ByteArray(mtuDivisible)
            for (i in pack until pack + mtuDivisible) {
                writearray[j] = otaFile!![i]
                j++
            }
            pgss = ((pack + mtuDivisible).toFloat() / (otaFile?.size!! - 1)) * 100
        }
        val charac = bluetoothGatt?.getService(OTA_SERVICE_UUID)?.getCharacteristic(OTA_DATA_UUID)
        charac?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        charac?.value = writearray
        bluetoothGatt?.writeCharacteristic(charac)
        val waiting_time = (System.currentTimeMillis() - otaTime)
        val bitrate = 8 * pack.toFloat() / waiting_time
        if (pack > 0) {
            Timber.d("progress = ${pgss.toInt()} %")
            val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
            Timber.d("datarate = $datarate")

        } else {
            otaTime = System.currentTimeMillis()
        }
    }

    @Synchronized
    fun writeOtaData(datathread: ByteArray?) {
        try {
            val value = ByteArray(mMtu - 3)
            val start = System.nanoTime()
            var j = 0
            for (i in datathread?.indices!!) {
                value[j] = datathread[i]
                j++
                if (j >= mMtu - 3 || i >= (datathread.size - 1)) {
                    var wait = System.nanoTime()
                    val charac = bluetoothGatt?.getService(OTA_SERVICE_UUID)?.getCharacteristic(OTA_DATA_UUID)
                    charac?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    val progress = ((i + 1).toFloat() / datathread.size) * 100
                    val bitrate = (((i + 1) * (8.0)).toFloat() / (((wait - start) / 1000000.0).toFloat()))
                    if (j < mMtu - 3) {
                        val end = ByteArray(j)
                        System.arraycopy(value, 0, end, 0, j)
                        Log.d("Progress", "sent " + (i + 1) + " / " + datathread.size + " - " + String.format("%.1f", progress) + " % - " + String.format("%.2fkbit/s", bitrate) + " - " + (end))
//                        runOnUiThread {
//                            datasize?.text = progress.toInt().toString() + " %"
//                            progressBar?.progress = progress.toInt()
//                        }
                        charac?.value = end
                    } else {
                        j = 0
                        Log.d("Progress", "sent " + (i + 1) + " / " + datathread.size + " - " + String.format("%.1f", progress) + " % - " + String.format("%.2fkbit/s", bitrate) + " - " + (value))
//                        runOnUiThread {
//                            datasize?.text = progress.toInt().toString() + " %"
//                            progressBar?.progress = progress.toInt()
//                        }
                        charac?.value = value
                    }
                    if (bluetoothGatt?.writeCharacteristic(charac)!!) {
//                        runOnUiThread {
//                            val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
//                            dataRate?.text = datarate
//                        }
                        while ((System.nanoTime() - wait) / 1000000.0 < delayNoResponse);
                    } else {
                        do {
                            while ((System.nanoTime() - wait) / 1000000.0 < delayNoResponse);
                            wait = System.nanoTime()
//                            runOnUiThread {
//                                val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
//                                dataRate?.text = datarate
//                            }
                        } while (!bluetoothGatt?.writeCharacteristic(charac)!!)
                    }
                }
            }
            val end = System.currentTimeMillis()
            val time = (end - start) / 1000L.toFloat()
            Log.d("OTA Time - ", "" + time + "s")
//            runOnUiThread {
//                chrono?.stop()
//                uploadimage?.clearAnimation()
//                uploadimage?.visibility = View.INVISIBLE
//            }
            dfuMode("OTA_END")
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    /**
     * WRITES BYTE TO OTA CONTROL CHARACTERISTIC
     */
    private fun writeOtaControl(ctrl: Byte): Boolean {
        Timber.d("writeOtaControl: $ctrl")
        if (bluetoothGatt?.getService(OTA_SERVICE_UUID) != null) {
            val charac =
                bluetoothGatt?.getService(OTA_SERVICE_UUID)?.getCharacteristic(OTA_CONTROL_UUID)
            if (charac != null) {
                Timber.d("writeOtaControl: instanceId = ${charac.instanceId}")
                charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                Timber.d("writeOtaControl: characProperties = ${charac.properties}")
                val control = ByteArray(1)
                control[0] = ctrl
                charac.value = control
                bluetoothGatt?.writeCharacteristic(charac)
                return true
            } else {
                Timber.e("writeOtaControl: character null")
            }
        } else {
            Timber.e("writeOtaControl: service null")
        }
        return false
    }

    /**
     * WRITES OTA CONTROL FOR HOMEKIT DEVICES
     */
   private fun homeKitOTAControl(instanceID: ByteArray) {
        //WRITE CHARACTERISTIC FOR HOMEKIT
        val value = byteArrayOf(0x00, 0x02, 0xee.toByte(), instanceID[0], instanceID[1], 0x03, 0x00, 0x01, 0x01, 0x01)
        writeGenericCharacteristic(OTA_SERVICE_UUID, OTA_CONTROL_UUID, value)
        Timber.d("homeKitOTAControl: character writing : ${ConverterUtil.getHexValue(value)}")
    }

    /**
     * WRITES BYTE ARRAY TO A GENERIC CHARACTERISTIC
     */
    private fun writeGenericCharacteristic(service: UUID?, characteristic: UUID?, value: ByteArray?): Boolean {
        if (bluetoothGatt != null) {
            val bluetoothGattCharacteristic = bluetoothGatt?.getService(service)?.getCharacteristic(characteristic)
            Timber.d( "writeGenericCharacteristic: exists")
            if (bluetoothGattCharacteristic != null) {
                bluetoothGattCharacteristic.value = value
                bluetoothGattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                bluetoothGatt?.writeCharacteristic(bluetoothGattCharacteristic)
                Timber.d( "writeGenericCharacteristic: written")
            } else {
                Timber.e( "writeGenericCharacteristic: null")
                return false
            }
        } else {
            Timber.e( "writeGenericCharacteristic: bluetoothGatt null")
            return false
        }
        return true
    }



   private fun getServicesInfo(gatt: BluetoothGatt) {
        Timber.d("---- GATT SERVICE INFO ----")
        val gattServices = gatt.services
        Timber.d( "Services count: ${gattServices.size}")
        for (gattService: BluetoothGattService in gattServices) {
            val serviceUUID = gattService.uuid.toString()
            Timber.d( "Service UUID: $serviceUUID -- Char count: ${gattService.characteristics.size}")
            val gattCharacteristics = gattService.characteristics
            for (gattCharacteristic: BluetoothGattCharacteristic in gattCharacteristics) {
                val characteristicUUID = gattCharacteristic.uuid.toString()
                Timber.d( "Characteristic UUID: $characteristicUUID -- Properties: ${gattCharacteristic.properties}")
                if ((gattCharacteristic.uuid.toString() == OTA_CONTROL_UUID.toString())) {
                    if (gattCharacteristics.contains(bluetoothGatt?.getService(OTA_SERVICE_UUID)?.getCharacteristic(OTA_DATA_UUID))) {
                        if (!gattServices.contains(bluetoothGatt?.getService(HOME_KIT_SERVICE_UUID))) {
                            Log.i("onServicesDiscovered", "Device in DFU Mode")
                            Timber.d("onServiceDiscovered : Device in DFU Mode")
                        } else {
                            Timber.d("onServiceDiscovered :OTA_Control found")
                            val gattDescriptors = gattCharacteristic.descriptors
                            for (gattDescriptor: BluetoothGattDescriptor in gattDescriptors) {
                                val descriptor = gattDescriptor.uuid.toString()
                                if ((gattDescriptor.uuid.toString() == HOME_KIT_DESCRIPTOR_UUID.toString())) {
                                    kitDescriptor = gattDescriptor
                                    Log.i("descriptor", "UUID: $descriptor")
                                    //bluetoothGatt.readDescriptor(gattDescriptor);
                                    val stable = byteArrayOf(0x00.toByte(), 0x00.toByte())
                                    homeKitOTAControl(stable)
                                    isHomekit = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ScanCallback
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        super.onScanResult(callbackType, result)
        synchronized(leScanStarted) {
            if (!leScanStarted) {
                return
            }
        }
        scanCallbacks.forEach { callback ->
            callback.onScanResult(callbackType, result)
        }
    }

    // BluetoothStateListener

    override fun onBluetoothStateChanged(enabled: Boolean) {
        synchronized(leScanStarted) {
            Timber.d("onBluetoothStateChanged")
            if (leScanStarted) {
                leScanStarted = false
            }
        }
    }

    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_DISCONNECTED && status != BluetoothGatt.GATT_SUCCESS) {
                Timber.d("onConnectionStateChange: status = $status")
                otaConfigListeners.forEach { listener ->
                    listener.onGattConnectFailed(status)
                }
            } else if (newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("GATT Server connected, attempt to start discovery services")
                bluetoothGatt?.discoverServices()
                otaConfigListeners.forEach { listener ->
                    listener.onGattConnected()
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Timber.d("STATE_DISCONNECTED")
                otaConfigListeners.forEach { listener ->
                    listener.onGattDisconnected()
                }
                bluetoothGatt!!.close()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Timber.d("onMtuChanged: $status")
            if (status == 0) {
                mMtu = mtu
                bluetoothGatt?.requestConnectionPriority(mPriority)
                dfuMode("OTA_BEGIN")
            } else {
                Timber.e("requestMTU failed: $status = $status")
                isOtaProcess = false
                handler.postDelayed({  bluetoothGatt!!.disconnect() }, 2000)

            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != 0) {
                Timber.e("onServiceDiscovered failed :status = $status")
                bluetoothGatt?.close()
            } else {
                getServicesInfo(gatt) //SHOW SERVICES IN LOG

                //DEFINE IF DEVICE SUPPORT OTA & MODE (NORMAL/DFU)
                val otaServiceCheck = gatt.getService(OTA_SERVICE_UUID) != null
                if (otaServiceCheck) {
                    val otaDataCheck = gatt.getService(OTA_SERVICE_UUID).getCharacteristic(OTA_DATA_UUID) != null
                    if(otaDataCheck) {
                        val homekitCheck = gatt.getService(HOME_KIT_SERVICE_UUID) != null
                        if(!homekitCheck){
                            isOtaMode = true
                            val otaDataProperty = gatt.getService(OTA_SERVICE_UUID).getCharacteristic(OTA_DATA_UUID).properties
                            if ((otaDataProperty == 12) || (otaDataProperty == 8) || (otaDataProperty == 10)) {
                                //mIsReliable = true;
                            } else if (isOtaMode && otaDataProperty == 4) {
                                //mIsReliable = false;
                            }
                        }
                    } else {
                        if(isOtaBegin) writeOtaControl(0x00.toByte())

                        //REQUEST MTU
                        bluetoothGatt?.requestMtu(mMtu)

                        //IF DFU_MODE, LAUNCH OTA PROGRESS AUTOMATICALLY
                        if (isOtaMode && isOtaBegin) {
                           dfuMode("OTA_BEGIN")
                        }
                    }
                }

            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
            Timber.d("onCharacteristicWrite:$status")
            if (characteristic.value.size < 10) Timber.d(String.format("onCharacteristicWrite: uuid=${characteristic.uuid} -- value =${ConverterUtil.getHexValue(characteristic.value)} -- status = $status"))

            if (status != 0) {
                Timber.e("onCharacteristicWrite: error status = $status")
                bluetoothGatt!!.close()
            } else {
                if ((characteristic.uuid == OTA_CONTROL_UUID)) { //OTA Control Callback Handling
                    if (characteristic.value.size == 1) {
                        if (characteristic.value[0] == 0x00.toByte()) { // Start OTA uploading process
                            Timber.d("onCharacteristicWrite: starting upload")
                            handler.removeCallbacks(DFU_OTA_UPLOAD)
                            handler.postDelayed(DFU_OTA_UPLOAD, 500)
                        }
                        if (characteristic.value[0] == 0x03.toByte()) { //Finish uploading process
                            Timber.d(String.format("onCharacteristicWrite = uploading finish"))
                            if(isOtaProcess) {
                                isOtaBegin = false
                            }
                        }
                    } else { //Homekit
                        Timber.d(String.format("onCharacteristicWrite homekit value = ${characteristic.value}"))
                        if (characteristic.value[0] == 0x00.toByte() && characteristic.value[1] == 0x02.toByte()) {
                            Timber.d(String.format("onCharacteristicWrite homekit ota control"))
                            bluetoothGatt?.readCharacteristic(characteristic)
                        }
                    }
                }

                if ((characteristic.uuid == OTA_DATA_UUID)) {   //OTA Data Callback Handling
                    if (mIsReliable) {
                            pack += mtuDivisible
                            if (pack <= otaFile?.size!! - 1) {
                                otaWriteDataReliable()
                            } else if (pack > otaFile?.size!! - 1) {
                                dfuMode("OTA_END")
                            }
                    }
                }
            }
            bluetoothGatt?.readCharacteristic(characteristic)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.i("Callback", "OnCharacteristicRead: " +(characteristic.value) + " Status: " + status)
            if (characteristic === (bluetoothGatt?.getService(OTA_SERVICE_UUID)?.getCharacteristic(OTA_CONTROL_UUID))) {
                val value = characteristic.value
                if (value[2] == 0x05.toByte()) {
                    Log.d("homekit_descriptor", "Insecure Connection")
                  //  runOnUiThread { showMessage("Error: Not a Homekit Secure Connection") }
                } else if (value[2] == 0x04.toByte()) {
                    Log.d("homekit_descriptor", "Wrong Address")
                } else if (value[2] == 0x00.toByte()) {
                    Log.d("homekit_descriptor", "Entering in DFU_Mode...")
//                    if (ota_mode && ota_process) {
//                        Log.d("OTAUPLOAD", "Sent")
//                        runOnUiThread(checkbeginrunnable)
//                        handler.removeCallbacks(DFU_OTA_UPLOAD)
//                        handler.postDelayed(DFU_OTA_UPLOAD, 500)
//                    } else if (!ota_mode && ota_process) {
//                        runOnUiThread {
//                            loadingLog?.text = "Resetting..."
//                            showLoading()
//                            animaloading()
//                            Constants.ota_button?.isVisible = true
//                        }
//                        handler.postDelayed({ reconnect(4000) }, 200)
//                    }
                }
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor, status: Int) {
            if ((descriptor.uuid.toString() == HOME_KIT_DESCRIPTOR_UUID.toString())) {
                val value = ByteArray(2)
                value[0] = 0xF2.toByte()
                value[1] = 0xFF.toByte()
                if (descriptor.value[0] == value[0] && descriptor.value[1] == value[1]) {
                    Timber.d("onDescriptorRead: getValue = ${ConverterUtil.getHexValue(descriptor.value)}")
                    homeKitOTAControl(descriptor.value)
                }
            }
        }
    }
}