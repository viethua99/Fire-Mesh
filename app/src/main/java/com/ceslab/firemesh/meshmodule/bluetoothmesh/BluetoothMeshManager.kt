package com.ceslab.firemesh.meshmodule.bluetoothmesh

import android.content.Context
import com.ceslab.firemesh.meshmodule.model.MeshConnectableDevice
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.myapp.*
import com.siliconlab.bluetoothmesh.adk.BluetoothMesh
import com.siliconlab.bluetoothmesh.adk.configuration.BluetoothMeshConfiguration
import com.siliconlab.bluetoothmesh.adk.configuration.BluetoothMeshConfigurationLimits
import com.siliconlab.bluetoothmesh.adk.configuration.LocalVendorModel
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import com.siliconlab.bluetoothmesh.adk.data_model.network.Network
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import com.siliconlab.bluetoothmesh.adk.notification_control.LocalVendorRegistrator
import com.siliconlab.bluetoothmesh.adk.notification_control.settings.LocalVendorSettings
import com.siliconlab.bluetoothmesh.adk.notification_control.settings.LocalVendorSettingsMessageHandler
import timber.log.Timber

/**
 * Created by Viet Hua on 11/23/2020.
 */

class BluetoothMeshManager(context: Context) {

    companion object {
        private const val NETWORK_MAX = 7
        private const val GROUP_MAX = 8
        private const val NODE_CAN_PROVISIONED_MAX = 100
        private const val NETWORK_KEY_SINGLE_NODE_HOLD_MAX = 7
        private const val APPLICATION_KEY_SINGLE_NODE_HOLD_MAX = 32

        private const val RPL_SIZE_MAX = 32
        private const val SEGMENT_MESSAGE_RECEIVED_MAX = 4
        private const val SEGMENT_MESSAGE_SENT_MAX = 4
        private const val PROVISION_SESSION_MAX = 1
    }


    val bluetoothMesh: BluetoothMesh
    var provisionedMeshConnectableDevice: MeshConnectableDevice? = null
    var meshNodeToConfigure: MeshNode? = null

    //Current data
    var currentNetwork: Network? = null
    var currentSubnet: Subnet? = null
    var currentGroup: Group? = null

    //Vendor models
    private val nodeStatusServer = LocalVendorModel(COMPANY_IDENTIFIER, NODE_STATUS_SERVER_ID)
    private val nodeStatusClient = LocalVendorModel(COMPANY_IDENTIFIER, NODE_STATUS_CLIENT_ID)
    private val gatewayStatusServer = LocalVendorModel(COMPANY_IDENTIFIER, GATEWAY_STATUS_SERVER_ID)
    private val gatewayStatusClient = LocalVendorModel(COMPANY_IDENTIFIER, GATEWAY_STATUS_CLIENT_ID)

    private fun initBluetoothMeshLimits() : BluetoothMeshConfigurationLimits {
        val bluetoothMeshLimits = BluetoothMeshConfigurationLimits()
        bluetoothMeshLimits.networks = NETWORK_MAX
        bluetoothMeshLimits.groups = GROUP_MAX
        bluetoothMeshLimits.nodes = NODE_CAN_PROVISIONED_MAX
        bluetoothMeshLimits.nodeNetworks = NETWORK_KEY_SINGLE_NODE_HOLD_MAX
        bluetoothMeshLimits.nodeGroups = APPLICATION_KEY_SINGLE_NODE_HOLD_MAX
        bluetoothMeshLimits.rplSize = RPL_SIZE_MAX
        bluetoothMeshLimits.segmentedMessagesReceived = SEGMENT_MESSAGE_RECEIVED_MAX
        bluetoothMeshLimits.segmentedMessagesSent = SEGMENT_MESSAGE_SENT_MAX
        bluetoothMeshLimits.provisionSessions = PROVISION_SESSION_MAX

        return bluetoothMeshLimits
    }



    private val bluetoothMeshConfiguration = BluetoothMeshConfiguration(
        listOf(
            nodeStatusServer,
            nodeStatusClient,
            gatewayStatusServer,
            gatewayStatusClient
        ),
        initBluetoothMeshLimits()
    )




    init {
        BluetoothMesh.initialize(context, bluetoothMeshConfiguration)
        bluetoothMesh = BluetoothMesh.getInstance()
        val opCodes = byteArrayOf(0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA)

        val localVendorSettingsMessageHandler =
            LocalVendorSettingsMessageHandler { _, _, _, _, _, _, _ -> }
        val localVendorSettings = LocalVendorSettings(opCodes, localVendorSettingsMessageHandler)
        val localVendorRegistrator = LocalVendorRegistrator(nodeStatusClient)
        localVendorRegistrator.registerSettings(localVendorSettings)
    }
}