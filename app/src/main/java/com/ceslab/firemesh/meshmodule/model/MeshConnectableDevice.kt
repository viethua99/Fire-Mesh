package com.ceslab.firemesh.meshmodule.model

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.ceslab.firemesh.meshmodule.bluetoothle.BluetoothScanner
import com.siliconlab.bluetoothmesh.adk.connectable_device.ConnectableDevice
import com.siliconlab.bluetoothmesh.adk.connectable_device.ConnectableDeviceSubscriptionCallback
import com.siliconlab.bluetoothmesh.adk.connectable_device.ConnectableDeviceWriteCallback
import com.siliconlab.bluetoothmesh.adk.connectable_device.RefreshBluetoothDeviceCallback
import timber.log.Timber
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Viet Hua on 11/23/2020.
 */
class MeshConnectableDevice(
    val context: Context,
    var scanResult: ScanResult,
    val bluetoothScanner: BluetoothScanner
) : ConnectableDevice() {


    lateinit var bluetoothDevice: BluetoothDevice
    lateinit var bluetoothGatt: BluetoothGatt

    lateinit var address: String
    lateinit var scanCallback: ScanCallback
    lateinit var bluetoothGattCallback: BluetoothGattCallback
    lateinit var refreshBluetoothDeviceTimeoutRunnable: Runnable

    private var advertisementData: ByteArray? = null
    private var connecting = false
    private var mtuSize = 0
    private var mainHandler = Handler(Looper.getMainLooper())
    private var deviceConnectionCallbacks = mutableSetOf<DeviceConnectionCallback>()
    private var refreshBluetoothDeviceCallback: RefreshBluetoothDeviceCallback? = null


    init {
        analyzeDataFromScanResult(scanResult)
        initScanCallback()
        initBluetoothGattCallback()
        initRefreshBluetoothDeviceTimeoutRunnable()

    }

    override fun refreshBluetoothDevice(refreshBluetoothDeviceCallback: RefreshBluetoothDeviceCallback) {
        Timber.d("refreshBluetoothDevice")
        if (startScan()) {
            Timber.d("refreshBluetoothDevice: starting scan succeeded")

            onScanStarted(refreshBluetoothDeviceCallback)
        } else {
            Timber.d("refreshBluetoothDevice: starting scan failed")

            refreshBluetoothDeviceCallback.failure()
        }
    }

    override fun getAdvertisementData() = advertisementData

    override fun getName(): String? {
        Timber.d("getName")
        return bluetoothDevice.name
    }

    override fun getMTU(): Int {
        Timber.d("getMTU $mtuSize")
        return mtuSize
    }

    override fun writeData(
        service: UUID?,
        characteristic: UUID?,
        data: ByteArray?,
        connectableDeviceWriteCallback: ConnectableDeviceWriteCallback
    ) {
        Timber.d("writeData")
        checkMainThread()
        try {
            tryToWriteData(service, characteristic, data)
            connectableDeviceWriteCallback.onWrite(service, characteristic)
        } catch (e: Exception) {
            Timber.d("writeData error: ${e.message}")
            connectableDeviceWriteCallback.onFailed(service, characteristic)
        }
    }

    override fun connect() {
        Timber.d("connect mac: $address")
        connectGatt()
        setupConnectionTimeout(bluetoothGatt)
    }

    override fun disconnect() {
        Timber.d("disconnect mac: $address")
        checkMainThread()
        connecting = false
        mainHandler.removeCallbacks(refreshBluetoothDeviceTimeoutRunnable)
        closeGatt()
        stopScan()
    }

    override fun hasService(service: UUID?): Boolean {
        Timber.d("hasService: $service")
        return scanResult.scanRecord?.serviceUuids?.contains(ParcelUuid(service)) ?: return false
    }

    override fun getServiceData(service: UUID?): ByteArray? {
        Timber.d("getServiceData")
        return service?.let { scanResult.scanRecord?.getServiceData(ParcelUuid(it)) }

    }

    override fun subscribe(
        service: UUID?,
        characteristic: UUID?,
        connectableDeviceSubscriptionCallback: ConnectableDeviceSubscriptionCallback
    ) {
        Timber.d("subscribe service=$service characteristic=$characteristic")
        checkMainThread()

        try {
            Timber.d("available services=" + bluetoothGatt.services?.map { it.uuid })

            tryToSubscribe(service, characteristic)
            connectableDeviceSubscriptionCallback.onSuccess(service, characteristic)
        } catch (e: Exception) {
            e.message?.let {
                Timber.e("subscribe error: $it")
            } ?: e.printStackTrace()
            connectableDeviceSubscriptionCallback.onFail(service, characteristic)
        }
    }

    fun addDeviceConnectionCallback(deviceConnectionCallback: DeviceConnectionCallback) {
        Timber.d("addDeviceConnectionCallback")
        synchronized(deviceConnectionCallbacks) {
            deviceConnectionCallbacks.add(deviceConnectionCallback)
        }
    }

    fun removeDeviceConnectionCallback(deviceConnectionCallback: DeviceConnectionCallback) {
        Timber.d("removeDeviceConnectionCallback")
        synchronized(deviceConnectionCallbacks) {
            deviceConnectionCallbacks.remove(deviceConnectionCallback)
        }
    }

    private fun analyzeDataFromScanResult(scanResult: ScanResult) {
        Timber.d("analyzeDataFromScanResult")
        this.scanResult = scanResult
        this.bluetoothDevice = scanResult.device
        this.advertisementData = scanResult.scanRecord!!.bytes
        this.address = scanResult.device.address
    }

    private fun initScanCallback() {
        Timber.d("initScanCallback")
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                Timber.d("onScanResult")
                result?.let {
                    if (it.device.address == address) {
                        analyzeDeviceFound(it)
                    }
                }
            }

            fun analyzeDeviceFound(scanResult: ScanResult) {
                Timber.d("analyzeDeviceFound")
                stopScan()
                mainHandler.removeCallbacks(refreshBluetoothDeviceTimeoutRunnable)
                analyzeDataFromScanResult(scanResult)
                // workaround to 133 gatt issue
                // https://github.com/googlesamples/android-BluetoothLeGatt/issues/44
                mainHandler.postDelayed({ refreshBluetoothDeviceCallback?.success() }, 500)
            }
        }
    }

    private fun initBluetoothGattCallback() {
        Timber.d("initBluetoothGattCallback")
        bluetoothGattCallback = object : BluetoothGattCallback() {
            private var connectionAttempts = 0
            private var changeMtuAttempts = 0
            private var discoverServicesAttempts = 0

            /*************CONNECTIONS STATE FUNCTIONS***********/
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                mainHandler.post {
                    Timber.d("onConnectionStateChange : status: $status, newState: $newState")
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        onConnectionStateChangeSuccess(gatt, newState)
                    } else {
                        onConnectionStateChangeFail(gatt)
                    }
                }
            }

            private fun onConnectionStateChangeSuccess(gatt: BluetoothGatt, newState: Int) {
                Timber.d("onConnectionStateChangeSuccess")
                connectionAttempts = 0

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    changeMtu(gatt)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED || newState == BluetoothProfile.STATE_DISCONNECTING) {
                    disconnect()
                }
            }

            private fun onConnectionStateChangeFail(gatt: BluetoothGatt) {
                Timber.d("onConnectionStateChangeFail")
                connecting = false
                gatt.close()

                if (!isConnected) {
                    onConnectionAttemptFail()
                } else {
                    onConnectionFail()
                }
            }

            private fun onConnectionAttemptFail() {
                Timber.d("onConnectionAttemptFail: $connectionAttempts")
                connectionAttempts++
                nextAttempt()
            }

            private fun nextAttempt() {
                Timber.d("nextAttempt: connectionAttempts: $connectionAttempts")
                if (connectionAttempts <= 3) {
                    //workaround to 133 gatt issue
                    connect()
                } else {
                    onConnectionError()
                    connectionAttempts = 0
                }
            }

            private fun onConnectionFail() {
                Timber.d("onConnectionFail")
                connectionAttempts = 0

                onConnectionError()
                notifyConnectionState(false)
            }

            /*************MTU CHANGED FUNCTIONS***********/

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
                mainHandler.post {
                    Timber.d("onMtuChanged : status $status, mtu: $mtu")
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        onMtuChangedSuccess(gatt, mtu)
                    } else {
                        onMtuChangedFail(gatt)
                    }
                }
            }

            private fun changeMtu(bluetoothGatt: BluetoothGatt) {
                Timber.d("changeMtu")
                for (i in 0..2) {
                    if (bluetoothGatt.requestMtu(512)) {
                        return
                    }
                    Thread.sleep(50)
                    Timber.d("retry request mtu i: $i")
                }
                discoverServices(bluetoothGatt)
            }

            private fun onMtuChangedSuccess(gatt: BluetoothGatt, mtu: Int) {
                Timber.d("onMtuChangedSuccess")
                changeMtuAttempts = 0
                mtuSize = mtu
                discoverServices(gatt)
            }

            private fun onMtuChangedFail(gatt: BluetoothGatt) {
                Timber.d("onMtuChangedFail")
                if (++changeMtuAttempts < 3) {
                    changeMtu(gatt)
                } else {
                    discoverServices(gatt)
                }
            }

            /*************SERVICES DISCOVERED FUNCTIONS***********/

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                mainHandler.post {
                    Timber.d("onServicesDiscovered: status: $status")
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        onServicesDiscoveredSuccess()
                    } else {
                        onServicesDiscoveredFail(gatt)
                    }
                }
            }

            private fun discoverServices(bluetoothGatt: BluetoothGatt) {
                Timber.d("discoverServices")
                for (i in 0..2) {
                    if (bluetoothGatt.discoverServices()) {
                        return
                    }
                    Thread.sleep(50)
                    Timber.d("retry discover services i: $i")

                }
                disconnect()
            }

            private fun onServicesDiscoveredSuccess() {
                Timber.d("onServicesDiscoveredSuccess")

                discoverServicesAttempts = 0
                if (connecting) {
                    //notify that device is connected only if it is connecting attempt
                    notifyDeviceConnected()
                }
            }

            private fun onServicesDiscoveredFail(gatt: BluetoothGatt) {
                Timber.d("onServicesDiscoveredFail")
                if (++discoverServicesAttempts < 3) {
                    discoverServices(gatt)
                } else {
                    disconnect()
                }
            }

            /*************CHARACTERISTICS CHANGED FUNCTIONS***********/

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                Timber.d("onCharacteristicChanged : bluetoothGattCharacteristic: ${characteristic.uuid}")
                super.onCharacteristicChanged(gatt, characteristic)
                updateData(characteristic.service.uuid, characteristic.uuid, characteristic.value)

            }
        }
    }

    private fun initRefreshBluetoothDeviceTimeoutRunnable() {
        Timber.d("initRefreshBluetoothDeviceTimeoutRunnable")
        refreshBluetoothDeviceTimeoutRunnable = Runnable {
            refreshingBluetoothDeviceTimeout()
        }
    }

    private fun refreshingBluetoothDeviceTimeout() {
        Timber.d("refreshingBluetoothDeviceTimeout")
        mainHandler.removeCallbacks(refreshBluetoothDeviceTimeoutRunnable)
        stopScan()
        refreshBluetoothDeviceCallback?.failure()
        refreshBluetoothDeviceCallback = null
    }

    private fun notifyConnectionState(connected: Boolean) {
        synchronized(deviceConnectionCallbacks) {
            for (callback in deviceConnectionCallbacks) {
                notifyConnectionState(callback, connected)
            }
        }
    }

    private fun notifyConnectionState(callback: DeviceConnectionCallback, connected: Boolean) {
        if (connected) {
            callback.onConnectedToDevice()
        } else {
            callback.onDisconnectedFromDevice()
        }
    }

    private fun notifyDeviceConnected() {
        Timber.d("notifyDeviceConnected")
        connecting = false
        onConnected()
        notifyConnectionState(true)
    }

    private fun notifyDeviceDisconnected() {
        Timber.d("notifyDeviceDisconnected")
        // workaround to 22 gatt issue (when try to connect immediately after disconnect)
        mainHandler.postDelayed({
            onDisconnected()
            notifyConnectionState(false)
        }, 500)
    }

    private fun startScan(): Boolean {
        Timber.d("startScan")
        bluetoothScanner.addScanCallback(scanCallback)
        return bluetoothScanner.startLeScan(null)
    }

    private fun stopScan() {
        Timber.d("stopScan")
        bluetoothScanner.removeScanCallback(scanCallback)
        bluetoothScanner.stopLeScan()
    }

    private fun connectGatt() {
        Timber.d("connectGatt")
        checkMainThread()
        bluetoothGatt = bluetoothDevice.connectGatt(
            context,
            false,
            bluetoothGattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
        bluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
    }

    private fun checkMainThread() {
        Timber.d("checkMainThread")
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw RuntimeException("Not on the main thread.")
        }
    }

    private fun setupConnectionTimeout(bluetoothGattLast: BluetoothGatt) {
        Timber.d("setupConnectionTimeout")
        connecting = true
        mainHandler.postDelayed({
            if (bluetoothGatt == bluetoothGattLast && connecting) {
                Timber.d("connection timeout mac: $address")
                onConnectionError()
            }
        }, TimeUnit.SECONDS.toMillis(30))
    }

    private fun tryToWriteData(service: UUID?, characteristic: UUID?, data: ByteArray?) {
        Timber.d("tryToWriteData")
        val bluetoothGattCharacteristic = getBluetoothGattCharacteristic(service, characteristic)
        setCharacteristicValueAndWriteType(bluetoothGattCharacteristic, data)
        writeCharacteristic(bluetoothGattCharacteristic)
    }

    private fun getBluetoothGattCharacteristic(
        service: UUID?,
        characteristic: UUID?
    ): BluetoothGattCharacteristic {
        Timber.d("getBluetoothGattCharacteristic")
        return bluetoothGatt.getService(service)!!.getCharacteristic(characteristic)
    }

    private fun setCharacteristicValueAndWriteType(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray?
    ) {
        Timber.d("setCharacteristicValueAndWriteType")
        characteristic.value = data
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
    }

    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        Timber.d("writeCharacteristic")
        if (!bluetoothGatt.writeCharacteristic(characteristic)) {
            throw Exception("Writing to characteristic failed")
        }
    }

    private fun tryToSubscribe(service: UUID?, characteristic: UUID?) {
        Timber.d("tryToSubscribe")
        val bluetoothGattCharacteristic =
            try {
                getBluetoothGattCharacteristic(service, characteristic)
            } catch (e: NullPointerException) {
                throw NullPointerException("Service not available")
            }
        enableCharacteristicNotification(bluetoothGattCharacteristic)
        val bluetoothGattDescriptor = getBluetoothGattDescriptor(bluetoothGattCharacteristic)
            .apply { value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE }
        writeDescriptor(bluetoothGattDescriptor)
    }

    private fun enableCharacteristicNotification(characteristic: BluetoothGattCharacteristic) {
        Timber.d("enableCharacteristicNotification")
        if (!bluetoothGatt.setCharacteristicNotification(characteristic, true)) {
            throw Exception("Enabling characteristic notification failed")
        }
    }

    private fun getBluetoothGattDescriptor(characteristic: BluetoothGattCharacteristic): BluetoothGattDescriptor {
        Timber.d("getBluetoothGattDescriptor")
        return characteristic.descriptors.takeIf { it.size == 1 }?.first()
            ?: throw Exception("Descriptors size (${characteristic.descriptors.size}) different than expected: 1")
    }

    private fun writeDescriptor(descriptor: BluetoothGattDescriptor) {
        Timber.d("writeDescriptor")
        if (!bluetoothGatt.writeDescriptor(descriptor)) {
            throw Exception("Writing to descriptor failed")
        }
    }

    private fun closeGatt() {
        Timber.d("closeGatt")
        if (this::bluetoothGatt.isInitialized) {
            refreshDeviceCache()
            bluetoothGatt.close()
            notifyDeviceDisconnected()
        }
    }

    private fun refreshDeviceCache() {
        Timber.d("refreshDeviceCache")
        try {
            val refreshMethod: Method = bluetoothGatt.javaClass.getMethod("refresh")
            val result = refreshMethod.invoke(bluetoothGatt, *arrayOfNulls(0)) as? Boolean
            Timber.d("refreshDeviceCache $result")
        } catch (localException: Exception) {
            Timber.d("An exception occured while refreshing device")


        }
    }

    private fun onScanStarted(callback: RefreshBluetoothDeviceCallback) {
        Timber.d("onScanStarted")
        refreshBluetoothDeviceCallback = callback
        mainHandler.removeCallbacks(refreshBluetoothDeviceTimeoutRunnable)
        mainHandler.postDelayed(refreshBluetoothDeviceTimeoutRunnable, 10000L)
    }

    interface DeviceConnectionCallback {
        fun onConnectedToDevice()

        fun onDisconnectedFromDevice()
    }
}