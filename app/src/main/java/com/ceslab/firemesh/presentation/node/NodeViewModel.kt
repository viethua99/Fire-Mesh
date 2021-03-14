package com.ceslab.firemesh.presentation.node

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.bluetoothmesh.BluetoothMeshManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshConnectionManager
import com.ceslab.firemesh.meshmodule.listener.ConnectionMessageListener
import com.ceslab.firemesh.meshmodule.listener.MeshLoadedListener
import com.ceslab.firemesh.meshmodule.listener.ConnectionStatusListener
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.ceslab.firemesh.util.ConverterUtil
import com.siliconlab.bluetoothmesh.adk.ErrorType
import timber.log.Timber
import javax.inject.Inject

class NodeViewModel @Inject constructor(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshConnectionManager: MeshConnectionManager
) : ViewModel() {
    private val advertise = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser

    var isFirstConfig = false
    private val meshStatus = MutableLiveData<MeshStatus>()
    private val connectionMessage = MutableLiveData<ConnectionMessageListener.MessageType>()
    private val errorMessage = MutableLiveData<ErrorType>()

    fun connectToNode() {
        Timber.d("connectToNode: $isFirstConfig")
        meshConnectionManager.apply {
            addMeshMessageListener(connectionMessageListener)
            addMeshConnectionListener(meshConnectionListener)
            addMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
        }

        if(isFirstConfig){
            Timber.d("isFirstConfig: true--- ${bluetoothMeshManager.meshNodeToConfigure!!.node.name}")
            meshConnectionManager.connect(bluetoothMeshManager.provisionedMeshConnectableDevice!!, true)
        }
    }

    fun disconnectFromNode(){
        Timber.d("disconnectFromNode")
        meshConnectionManager.apply {
            removeMeshMessageListener(connectionMessageListener)
            removeMeshConnectionListener(meshConnectionListener)
            removeMeshConfigurationLoadedListener(meshConfigurationLoadedListener)
        }

        if(isFirstConfig){
            meshConnectionManager.disconnect()
            stopAdvertise()
        }
    }

    fun getConnectionMessage(): LiveData<ConnectionMessageListener.MessageType> {
        return connectionMessage
    }

    fun getErrorMessage(): LiveData<ErrorType> {
        return errorMessage
    }

    fun getMeshStatus(): LiveData<MeshStatus> {
        return meshStatus
    }

    fun stopAdvertise() {
        Timber.d("stopAdvertise")
        advertise.stopAdvertising(advertiseCallback)
    }

    private fun startAdvertiseNodeUUID() {
        Timber.d("startAdvertiseNodeUUID")
        //TEST ADVERTISING
        val nodeUUID = bluetoothMeshManager.meshNodeToConfigure!!.node.uuid

        //Settings
        val advertiseSettingParams = AdvertiseSettings.Builder()
        advertiseSettingParams.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
        val settings = advertiseSettingParams.build()
        //DATA
        val advertiseDataParam = AdvertiseData.Builder()
        advertiseDataParam.setIncludeDeviceName(true)
            .addManufacturerData(767,nodeUUID)

        val data = advertiseDataParam.build()

        advertise.startAdvertising(settings,data,advertiseCallback)
    }



    private val meshConnectionListener = object : ConnectionStatusListener {
        override fun connecting() {
            Timber.d("connecting")
            meshStatus.value = MeshStatus.MESH_CONNECTING
        }

        override fun connected() {
            Timber.d("connected")
            if(isFirstConfig){
                meshConnectionManager.setupInitialNodeConfiguration(bluetoothMeshManager.meshNodeToConfigure!!.node)
            }
            meshStatus.value = MeshStatus.MESH_CONNECTED

        }

        override fun disconnected() {
            Timber.d("disconnected")
            meshStatus.value = MeshStatus.MESH_DISCONNECTED
        }
    }

    private val meshConfigurationLoadedListener = object : MeshLoadedListener {
        override fun initialConfigurationLoaded() {
            Timber.d("initialConfigurationLoaded")
            meshStatus.value = MeshStatus.INIT_CONFIGURATION_LOADED
            startAdvertiseNodeUUID()
        }
    }

    private val connectionMessageListener = object : ConnectionMessageListener {
        override fun connectionMessage(messageType: ConnectionMessageListener.MessageType) {
            Timber.d("connectionMessage: ${messageType.name}")
            connectionMessage.value = messageType

        }

        override fun connectionErrorMessage(error: ErrorType) {
            Timber.e("connectionErrorMessage: ${error.type}")
            errorMessage.value = error
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Timber.d("onStartSuccess")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Timber.e("onStartFailure: $errorCode")

        }
    }

}