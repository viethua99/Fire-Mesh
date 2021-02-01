package com.ceslab.ble_mesh_core

import android.os.Handler
import android.util.Log
import com.ceslab.ble_mesh_core.bluetoothle.BluetoothStateReceiver
import com.ceslab.ble_mesh_core.bluetoothle.LocationStateReceiver
import com.ceslab.data.core.BluetoothCore
import com.ceslab.domain.executor.ExecutionThread
import com.ceslab.domain.model.BluetoothStatus
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BluetoothCoreImpl @Inject constructor(
    private val bluetoothStateReceiver: BluetoothStateReceiver,
    private val locationStateReceiver: LocationStateReceiver
) : BluetoothCore {


    override fun checkBluetoothState(): Observable<BluetoothStatus> {
        return Observable.create {
            bluetoothStateReceiver.addListener(object : BluetoothStateReceiver.BluetoothStateListener {
                override fun onBluetoothStateChanged(enabled: Boolean) {
                    Log.d("BluetoothCoreImpl","onBluetoothStateChanged: $enabled")
                    it.onNext(BluetoothStatus.BLUETOOTH_STATUS_CHANGED)
                }
            })

            locationStateReceiver.addListener(object : LocationStateReceiver.LocationStateListener {
                override fun onLocationStateChanged() {
                    Log.d("BluetoothCoreImpl","onLocationStateChanged")
                    it.onNext(BluetoothStatus.LOCATION_STATUS_CHANGED)
                }
            })
        }
    }
}