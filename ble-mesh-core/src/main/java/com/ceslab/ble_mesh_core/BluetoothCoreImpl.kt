package com.ceslab.ble_mesh_core

import android.os.Handler
import android.util.Log
import com.ceslab.data.core.BluetoothCore
import io.reactivex.rxjava3.core.Completable
import javax.inject.Inject

class BluetoothCoreImpl @Inject constructor() : BluetoothCore {
    override fun testMethod(): Completable {
        Log.d("BluetoothCoreImpl","HERE")
        return Completable.create { emitter ->
            emitter.onComplete()
        }
    }
}