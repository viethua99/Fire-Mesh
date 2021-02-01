package com.ceslab.domain.repository

import com.ceslab.domain.model.BluetoothStatus
import io.reactivex.rxjava3.core.Observable

interface BluetoothRepository {
    fun checkBluetoothState(): Observable<BluetoothStatus>
}