package com.ceslab.firemesh.meshmodule.ota

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.ceslab.firemesh.meshmodule.bluetoothle.BluetoothStateReceiver
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class OTAManager(bluetoothStateReceiver: BluetoothStateReceiver) : ScanCallback(),
    BluetoothStateReceiver.BluetoothStateListener {

    private var handler: Handler = Handler()
    private val WRITE_OTA_CONTROL_ZERO = Runnable { writeOtaControl(0x00.toByte()) }
    private val DFU_OTA_UPLOAD = Runnable { dfuMode("OTA_UPLOAD") }
    private var otafile: ByteArray? = null
    private var pack = 0
    private var mtuDivisible = 0
    private var otatime: Long = 0
    private var delayNoResponse = 1


    // OTA file paths
    private var mAppPath = ""
    private var mStackPath = ""

    private val otaServiceUUID = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0")
    private val otaControlUUID = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063")
    private val otaDataUUID = UUID.fromString("984227f3-34fc-4045-a5d0-2c581f81a153")

    private var leScanStarted: Boolean = false
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val scanCallbacks = ConcurrentLinkedQueue<ScanCallback>()
    private val otaConfigListeners: ArrayList<OTAConfigListener> = ArrayList()

    //OTA PROCESS
    private var mMtu = 247
    private var mPriority = 2
    private var mIsReliable = true

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

    fun startOTAProcess(isReliable: Boolean, mtuValue: Int, priority: Int,stackPath:String,appPath:String) {
        mMtu = mtuValue
        mIsReliable = isReliable
        mPriority = priority
        mStackPath = stackPath
        mAppPath = appPath
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
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Timber.e("BluetoothAdapter not initialized or unspecified address")
            return
        }

        val device = bluetoothAdapter!!.getRemoteDevice(address)

        if (device == null) {
            Timber.e("Device not found. Unable to connect.")
        }
        bluetoothGatt = device.connectGatt(context, false, mGattCallback)
    }


    private fun initBluetoothLeScanner() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
    }


    private fun showOTAProgress() {
        Timber.d("showOTAProgress")

    }

    private fun dfuMode(step: String?) {
        when (step) {
            "OTA_BEGIN" -> {
                handler.postDelayed(WRITE_OTA_CONTROL_ZERO, 200)
            }
            "OTA_UPLOAD" -> {
                /**Check Services */
                val mBluetoothGattService = bluetoothGatt?.getService(otaServiceUUID)
                if (mBluetoothGattService != null) {
                    val charac = bluetoothGatt?.getService(otaServiceUUID)!!.getCharacteristic(otaDataUUID)
                    if (charac != null) {
                        charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        Log.d("Instance ID", "" + charac.instanceId)
                        /**Check Files */
                        var ebl: ByteArray? = null
                        try {
                            Log.d("stackPath", "" + mStackPath)
                            Log.d("appPath", "" + mAppPath)
                            val file: File
                            file = File(mAppPath)
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
                        val last = mAppPath.lastIndexOf(File.separator)
                        fn = mAppPath.substring(last)
                        Log.d("CurrentlyUpdating", "appliaction")

                        pack = 0
                        /**Prepare information about current upload step */
                        val stepInfo: String
                        stepInfo = "1 OF 1"

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
                            if (mIsReliable) {
                                otaWriteDataReliable()
                            } else whiteOtaData(datathread)
                        })
                        otaUpload.start()
                    }
                }
            }
            "OTA_END" -> {
                Log.d("OTAEND", "Called")
                handler.postDelayed({ writeOtaControl(0x03.toByte()) }, 500)
            }
        }
    }



    @Synchronized
    fun otaWriteDataReliable() {
        Log.d("Test","otaWriteDataReliable")
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
            Log.d("characte", "last: " + pack + " / " + (pack + last) + " : " + (writearray))
        } else {
            var j = 0
            writearray = ByteArray(mtuDivisible)
            for (i in pack until pack + mtuDivisible) {
                writearray[j] = otafile!![i]
                j++
            }
            pgss = ((pack + mtuDivisible).toFloat() / (otafile?.size!! - 1)) * 100
            Log.d("characte", "pack: " + pack + " / " + (pack + mtuDivisible) + " : " + (writearray))
        }
        val charac = bluetoothGatt?.getService(otaServiceUUID)?.getCharacteristic(otaDataUUID)
        charac?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        charac?.value = writearray
        bluetoothGatt?.writeCharacteristic(charac)
        val waiting_time = (System.currentTimeMillis() - otatime)
        val bitrate = 8 * pack.toFloat() / waiting_time
        if (pack > 0) {
//            handler.post {
//                runOnUiThread {
//                    progressBar?.progress = pgss.toInt()
//                    val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
//                    dataRate?.text = datarate
//                    datasize?.text = pgss.toInt().toString() + " %"
//                }
//            }
        } else {
            otatime = System.currentTimeMillis()
        }
    }

    @Synchronized
    fun whiteOtaData(datathread: ByteArray?) {
        try {
            val value = ByteArray(mMtu - 3)
            val start = System.nanoTime()
            var j = 0
            for (i in datathread?.indices!!) {
                value[j] = datathread[i]
                j++
                if (j >= mMtu - 3 || i >= (datathread.size - 1)) {
                    var wait = System.nanoTime()
                    val charac = bluetoothGatt?.getService(otaServiceUUID)?.getCharacteristic(otaDataUUID)
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
        Log.d("writeOtaControl", "Called")
        if (bluetoothGatt?.getService(otaServiceUUID) != null) {
            val charac =
                bluetoothGatt?.getService(otaServiceUUID)?.getCharacteristic(otaControlUUID)
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
                showOTAProgress()

            } else {
                Timber.e("requestMTU failed: $status = $status")
                bluetoothGatt!!.disconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == 0) {
                Timber.d("onServiceDiscovered failed :status = $status")
            } else {
                val otaServiceCheck = gatt.getService(otaServiceUUID) != null
                if (otaServiceCheck) {
                    //Working Process
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
            Timber.d("onCharacteristicWrite:$status")
            if (status != 0) {
                bluetoothGatt!!.close()
            } else {
                if ((characteristic.uuid == otaControlUUID)) { //OTA Control Callback Handling
                    if (characteristic.value.size == 1) {
                        if (characteristic.value[0] == 0x00.toByte()) {
                            handler.removeCallbacks(DFU_OTA_UPLOAD)
                            handler.postDelayed(DFU_OTA_UPLOAD, 500)
                        }
                        if (characteristic.value[0] == 0x03.toByte()) {
                            Log.d("Callback", "Control " + (characteristic.value) + "status: " + status)
                        }
                    } else {
                        Log.i(
                            "OTA_Control", "Received: " +(characteristic.value))
                        if (characteristic.value[0] == 0x00.toByte() && characteristic.value[1] == 0x02.toByte()) {
                            Log.i("HomeKit", "Reading OTA_Control...")
                            bluetoothGatt?.readCharacteristic(characteristic)
                        }
                    }
                }

                if ((characteristic.uuid == otaDataUUID)) {   //OTA Data Callback Handling
                    if (mIsReliable) {
                            pack += mtuDivisible
                            if (pack <= otafile?.size!! - 1) {
                                otaWriteDataReliable()
                            } else if (pack > otafile?.size!! - 1) {
//                                handler.post {
//                                    runOnUiThread {
//                                        chrono?.stop()
//                                        uploadimage?.clearAnimation()
//                                        uploadimage?.visibility = View.INVISIBLE
//                                    }
//                                }
                                dfuMode("OTA_END")
                            }
                    }
                }
            }
            bluetoothGatt?.readCharacteristic(characteristic)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.i("Callback", "OnCharacteristicRead: " +(characteristic.value) + " Status: " + status)
            if (characteristic === (bluetoothGatt?.getService(otaServiceUUID)?.getCharacteristic(otaControlUUID))) {
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

    }
}