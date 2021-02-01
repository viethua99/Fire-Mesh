package com.ceslab.data.core

import com.ceslab.domain.model.BluetoothStatus
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable

interface BluetoothCore {
    fun checkBluetoothState() : Observable<BluetoothStatus>
}