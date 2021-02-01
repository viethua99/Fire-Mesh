package com.ceslab.domain.interactor

import com.ceslab.domain.base.CompletableUseCase
import com.ceslab.domain.executor.ExecutionThread
import com.ceslab.domain.repository.BluetoothRepository
import io.reactivex.rxjava3.core.Completable
import javax.inject.Inject

class TestUseCase @Inject constructor(
    executionThread: ExecutionThread,
    private val bluetoothRepository: BluetoothRepository
) : CompletableUseCase<String>(executionThread) {

    override fun buildUseCase(t: String): Completable {
        return bluetoothRepository.testMethod()
    }
}