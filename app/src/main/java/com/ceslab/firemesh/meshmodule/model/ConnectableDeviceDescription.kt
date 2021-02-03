package com.ceslab.firemesh.meshmodule.model

import android.bluetooth.le.ScanResult
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node

/**
 * Created by Viet Hua on 11/23/2020.
 */

class ConnectableDeviceDescription {
    var deviceName: String? = null
    var deviceAddress: String? = null
    var rssi: Int = 0
    var tx: Int = 0
    var timeStamp: Long = 0
    var position = 0
    var scanRecord: ByteArray? = null
    var meshConnectableDevice: MeshConnectableDevice? = null
    var existedNode: Node? = null

    class Builder {
        companion object {
            fun build(
                result: ScanResult,
                device: MeshConnectableDevice
            ): ConnectableDeviceDescription {
                val connectableDeviceDescription = ConnectableDeviceDescription()
                connectableDeviceDescription.deviceAddress = result.device.address
                connectableDeviceDescription.deviceName = result.device.name
                connectableDeviceDescription.rssi = result.rssi
                connectableDeviceDescription.meshConnectableDevice = device
                connectableDeviceDescription.tx = result.scanRecord!!.txPowerLevel
                connectableDeviceDescription.timeStamp = result.timestampNanos
                connectableDeviceDescription.scanRecord = result.scanRecord!!.bytes
                return connectableDeviceDescription
            }
        }
    }
}