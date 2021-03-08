package com.ceslab.firemesh.meshmodule.bluetoothmesh

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.ceslab.firemesh.meshmodule.bluetoothle.BluetoothScanner
import com.ceslab.firemesh.meshmodule.listener.MeshLoadedListener
import com.ceslab.firemesh.meshmodule.listener.ConnectionStatusListener
import com.ceslab.firemesh.meshmodule.listener.ConnectionMessageListener
import com.ceslab.firemesh.meshmodule.model.MeshConnectableDevice
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.configuration_control.ConfigurationControl
import com.siliconlab.bluetoothmesh.adk.configuration_control.GetDeviceCompositionDataCallback
import com.siliconlab.bluetoothmesh.adk.configuration_control.SetNodeBehaviourCallback
import com.siliconlab.bluetoothmesh.adk.connectable_device.*
import com.siliconlab.bluetoothmesh.adk.data_model.dcd.DeviceCompositionData
import com.siliconlab.bluetoothmesh.adk.data_model.element.Element
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import timber.log.Timber
import java.util.ArrayList

/**
 * Created by Viet Hua on 11/23/2020.
 */
class MeshConnectionManager(

    val context: Context,
    val bluetoothScanner: BluetoothScanner,
    val connectableDeviceHelper: ConnectableDeviceHelper
) : MeshConnectableDevice.DeviceConnectionCallback {

    enum class CONNECTION_STATE {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private val uiHandler: Handler = Handler(Looper.getMainLooper())

    private var proxyConnection: ProxyConnection? = null
    private var meshConnectableDevice: MeshConnectableDevice? = null
    private var networkInfo: Subnet? = null
    private var currentState =
        CONNECTION_STATE.DISCONNECTED
    private val connectionStatusListeners: ArrayList<ConnectionStatusListener> = ArrayList()
    private val connectionMessageListeners: ArrayList<ConnectionMessageListener> = ArrayList()
    private val meshLoadedListeners: ArrayList<MeshLoadedListener> = ArrayList()

    private var connectionTimeoutRunnable: Runnable = Runnable {
        connectionTimeout()
    }

    override fun onConnectedToDevice() {
        Timber.d("onConnectedToDevice")
    }

    override fun onDisconnectedFromDevice() {
        Timber.d("onDisconnectedFromDevice")
        disconnect()
    }

    fun connect(network: Subnet) {
        Timber.d("connect")
        synchronized(this) {
            if (networkInfo != null) {
                // new network
                if (networkInfo != network) {
                    disconnect()
                } else {
                    // already connected/connecting
                    if (currentState != CONNECTION_STATE.DISCONNECTED) {
                        return
                    }
                }
            }
            Timber.d("Connecting to subnet")
            setCurrentState(CONNECTION_STATE.CONNECTING)

            networkInfo = network
            bluetoothScanner.addScanCallback(scanCallback)
            startScan()
        }
    }

    fun connect(meshConnectableDevice: MeshConnectableDevice, refreshBluetoothDevice: Boolean) {
        Timber.d("connect")
        synchronized(currentState) {
            if (networkInfo != null) {
                disconnect()
            }
            Timber.d("Connecting to device")
            setCurrentState(CONNECTION_STATE.CONNECTING)

            // workaround to 133 gatt issue
            // https://github.com/googlesamples/android-BluetoothLeGatt/issues/44
            uiHandler.postDelayed({
                this.meshConnectableDevice = meshConnectableDevice
                meshConnectableDevice.addDeviceConnectionCallback(this)

                proxyConnection = ProxyConnection(meshConnectableDevice)
                proxyConnection!!.connectToProxy(refreshBluetoothDevice, object :
                    ConnectionCallback {
                    override fun success(device: ConnectableDevice) {
                        Timber.d("ConnectionCallback success")
                        setCurrentState(CONNECTION_STATE.CONNECTED)
                    }

                    override fun error(device: ConnectableDevice, error: ErrorType) {
                        Timber.d("ConnectionCallback error=$error")
                        setCurrentState(CONNECTION_STATE.DISCONNECTED)
                        connectionErrorMessage(error)
                    }
                })
            }, 500)
        }
    }

    fun disconnect() {
        Timber.d("disconnect")
        networkInfo = null
        stopScan()
        bluetoothScanner.removeScanCallback(scanCallback)
        setCurrentState(CONNECTION_STATE.DISCONNECTED)
        meshConnectableDevice?.removeDeviceConnectionCallback(this)
        meshConnectableDevice = null
        proxyConnection?.disconnect(object : DisconnectionCallback {
            override fun success(device: ConnectableDevice?) {
                Timber.d("Disconnecting Success")
            }

            override fun error(device: ConnectableDevice?, error: ErrorType?) {
                Timber.d("Disconnecting error: $error")
            }
        })
    }

    fun addMeshConnectionListener(connectionStatusListener: ConnectionStatusListener) {
        synchronized(connectionStatusListeners) {
            connectionStatusListeners.add(connectionStatusListener)
            notifyCurrentState()
        }
    }

    fun removeMeshConnectionListener(connectionStatusListener: ConnectionStatusListener) {
        synchronized(connectionStatusListeners) {
            connectionStatusListeners.remove(connectionStatusListener)
        }
    }

    fun addMeshMessageListener(connectionMessageListener: ConnectionMessageListener) {
        synchronized(connectionMessageListeners) {
            connectionMessageListeners.add(connectionMessageListener)
        }
    }

    fun removeMeshMessageListener(connectionMessageListener: ConnectionMessageListener) {
        synchronized(connectionMessageListeners) {
            connectionMessageListeners.remove(connectionMessageListener)
        }
    }

    fun addMeshConfigurationLoadedListener(meshLoadedListener: MeshLoadedListener) {
        synchronized(meshLoadedListeners) {
            meshLoadedListeners.add(meshLoadedListener)
        }
    }

    fun removeMeshConfigurationLoadedListener(meshLoadedListener: MeshLoadedListener) {
        synchronized(meshLoadedListeners) {
            meshLoadedListeners.remove(meshLoadedListener)
        }
    }

    fun getCurrentlyConnectedNode(): Node? {
        return connectableDeviceHelper.findNode(meshConnectableDevice)
    }


    fun setupInitialNodeConfiguration(node: Node) {
        Timber.d("setupInitialNodeConfiguration")
        enableProxy(node)
    }

    private fun enableProxy(node: Node) {
        Timber.d("enableProxy")
        ConfigurationControl(node).setProxy(true, object : SetNodeBehaviourCallback {
            override fun error(node: Node, error: ErrorType) {
                connectionErrorMessage(error)
            }

            override fun success(node: Node, enabled: Boolean) {
                Timber.d("success: node=${node.name}")
                getDeviceCompositionData(node)

            }
        })
    }

    private fun getDeviceCompositionData(node: Node) {
        Timber.d("getDeviceCompositionData")
        ConfigurationControl(node).getDeviceCompositionData(0, object :
            GetDeviceCompositionDataCallback {
            override fun error(node: Node, error: ErrorType) {
                connectionErrorMessage(error)
            }

            override fun success(
                node: Node,
                deviceCompositionData: DeviceCompositionData,
                elements: Array<out Element>?
            ) {
                enableNodeIdentity(node)
            }
        })
    }

    private fun enableNodeIdentity(node: Node) {
        Timber.d("enableNodeIdentity")
        ConfigurationControl(node).setNodeIdentity(true, node.subnets.first(), object :
            SetNodeBehaviourCallback {
            override fun error(node: Node, error: ErrorType) {
                Timber.d("error: $error")
                connectionErrorMessage(error)
            }

            override fun success(node: Node, enabled: Boolean) {
                meshLoadedListeners.forEach { listener -> listener.initialConfigurationLoaded() }
            }
        })
    }


    private fun startScan() {
        Timber.d("startScan")
        networkInfo?.apply {
            if (nodes.isEmpty()) {
                connectionMessage(ConnectionMessageListener.MessageType.NO_NODE_IN_NETWORK)
                return
            }

            val meshProxyService = ParcelUuid(ProxyConnection.MESH_PROXY_SERVICE)
            if (bluetoothScanner.startLeScan(meshProxyService)) {
                uiHandler.removeCallbacks(connectionTimeoutRunnable)
                uiHandler.postDelayed(connectionTimeoutRunnable, 10000)
            }
        }
    }

    private fun stopScan() {
        Timber.d("stopScan")
        bluetoothScanner.stopLeScan()
    }

    private fun setCurrentState(currentState: CONNECTION_STATE) {
        Timber.d("setCurrentState: $currentState")
        synchronized(this) {
            if (this.currentState == currentState) {
                return
            }
            uiHandler.removeCallbacks(connectionTimeoutRunnable)
            this.currentState = currentState
        }
        notifyCurrentState()
    }

    private fun notifyCurrentState() {
        Timber.d("notifyCurrentState")
        synchronized(connectionStatusListeners) {
            uiHandler.post {
                when (currentState) {
                    CONNECTION_STATE.DISCONNECTED -> {
                        connectionStatusListeners.forEach { listener -> listener.disconnected() }
                    }
                    CONNECTION_STATE.CONNECTING -> {
                        connectionStatusListeners.forEach { listener -> listener.connecting() }
                    }
                    CONNECTION_STATE.CONNECTED -> {
                        connectionStatusListeners.forEach { listener -> listener.connected() }
                    }
                }
            }
        }
    }

    private fun connectionTimeout() {
        Timber.d("connectionTimeout")
        stopScan()
        setCurrentState(CONNECTION_STATE.DISCONNECTED)
        connectionErrorMessage(ErrorType(ErrorType.TYPE.COULD_NOT_CONNECT_TO_DEVICE))
    }

    private fun connectionMessage(message: ConnectionMessageListener.MessageType) {
        synchronized(connectionMessageListeners) {
            uiHandler.post {
                connectionMessageListeners.forEach { listener -> listener.connectionMessage(message) }
            }
        }
    }

    private fun connectionErrorMessage(errorType: ErrorType) {
        synchronized(connectionMessageListeners) {
            uiHandler.post {
                connectionMessageListeners.forEach { listener -> listener.connectionErrorMessage(errorType) }
            }
        }
    }


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Timber.d("onScanResult: ${result.toString()}")

            val bluetoothConnectableDevice =
                MeshConnectableDevice(context, result, bluetoothScanner)

            val subnets = connectableDeviceHelper.findSubnets(bluetoothConnectableDevice)

            if (subnets.contains(networkInfo)) {
                stopScan()
            } else {
                return
            }

            connect(bluetoothConnectableDevice, refreshBluetoothDevice = false)
        }
    }

}