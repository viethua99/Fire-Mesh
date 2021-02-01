package com.ceslab.data

import com.ceslab.data.core.BluetoothCore
import com.ceslab.domain.model.BluetoothStatus
import com.ceslab.domain.repository.BluetoothRepository
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class BluetoothRepositoryImpl @Inject constructor(private val bluetoothCore: BluetoothCore) :
    BluetoothRepository {

    override fun checkBluetoothState(): Observable<BluetoothStatus> {
       return bluetoothCore.checkBluetoothState()
    }
}