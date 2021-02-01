package com.ceslab.domain.interactor

import com.ceslab.domain.base.ObservableUseCase
import com.ceslab.domain.executor.ExecutionThread
import com.ceslab.domain.model.BluetoothStatus
import com.ceslab.domain.model.EmptyParam
import com.ceslab.domain.repository.BluetoothRepository
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class BluetoothStateUseCase @Inject constructor(executionThread: ExecutionThread, private val bluetoothRepository: BluetoothRepository): ObservableUseCase<EmptyParam>(executionThread){
    override fun buildUseCase(t: EmptyParam): Observable<BluetoothStatus> {
        return bluetoothRepository.checkBluetoothState()
    }
}