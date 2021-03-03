package com.ceslab.firemesh.ota.callbacks

import android.bluetooth.BluetoothGattCallback

abstract class TimeoutGattCallback : BluetoothGattCallback() {
    open fun onTimeout() {}
}