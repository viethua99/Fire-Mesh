package com.ceslab.firemesh.meshmodule.bluetoothmesh

import android.content.Context
import com.ceslab.firemesh.meshmodule.model.MeshConnectableDevice
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.siliconlab.bluetoothmesh.adk.BluetoothMesh
import com.siliconlab.bluetoothmesh.adk.configuration.BluetoothMeshConfiguration
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
    val bluetoothMesh: BluetoothMesh
    var provisionedMeshConnectableDevice: MeshConnectableDevice? = null
    var meshNodeToConfigure: MeshNode? = null

    //Current data
    var currentNetwork: Network? = null
    var currentSubnet: Subnet? = null
    var currentGroup: Group? = null

    //Vendor models
    private val myVendorModelServer = LocalVendorModel(4369, 4369)
    private val myVendorModelClient = LocalVendorModel(4369, 8738)
    private val gatewayStatusModelServer = LocalVendorModel(4369, 13107)
    private val gatewayStatusModelClient = LocalVendorModel(4369, 17476)

    val bluetoothMeshConfiguration = BluetoothMeshConfiguration(
        listOf(
            myVendorModelServer,
            myVendorModelClient,
            gatewayStatusModelServer,
            gatewayStatusModelClient
        )
    )


    init {
        BluetoothMesh.initialize(context, bluetoothMeshConfiguration)
        bluetoothMesh = BluetoothMesh.getInstance()
        val opCodes = byteArrayOf(0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA)

        val localVendorSettingsMessageHandler =
            LocalVendorSettingsMessageHandler { p0, p1, p2, p3, p4, p5, p6 ->
                Timber.d("Vendor Setting Handler: $p5")
            }
        val localVendorSettings = LocalVendorSettings(opCodes, localVendorSettingsMessageHandler)
        val localVendorRegistrator = LocalVendorRegistrator(myVendorModelClient)
        localVendorRegistrator.registerSettings(localVendorSettings)
    }
}