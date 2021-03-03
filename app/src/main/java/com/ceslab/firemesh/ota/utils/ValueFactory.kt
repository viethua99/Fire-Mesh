package com.ceslab.firemesh.ota.utils

import android.bluetooth.BluetoothGattCharacteristic

interface ValueFactory<T> {
    fun create(value: BluetoothGattCharacteristic): T
}