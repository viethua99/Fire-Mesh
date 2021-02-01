package com.ceslab.data

import com.ceslab.data.core.BluetoothCore
import com.ceslab.domain.repository.BluetoothRepository
import io.reactivex.rxjava3.core.Completable
import javax.inject.Inject

class BluetoothRepositoryImpl @Inject constructor(private val bluetoothCore: BluetoothCore) :
    BluetoothRepository {
    override fun testMethod(): Completable {
        return bluetoothCore.testMethod()
    }
}