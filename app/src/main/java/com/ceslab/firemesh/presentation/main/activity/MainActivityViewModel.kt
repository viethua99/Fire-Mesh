package com.ceslab.firemesh.presentation.main.activity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.domain.interactor.BluetoothStateUseCase
import com.ceslab.domain.model.BluetoothStatus
import com.ceslab.domain.model.EmptyParam
import io.reactivex.rxjava3.observers.DisposableObserver
import timber.log.Timber
import javax.inject.Inject

class MainActivityViewModel @Inject constructor() : ViewModel() {
    val bluetoothStatus = MutableLiveData<BluetoothStatus>()

    @Inject
    lateinit var bluetoothStateUseCase: BluetoothStateUseCase

    fun checkBluetoothState() {
        Timber.d("checkBluetoothState")
        bluetoothStateUseCase.execute(object : DisposableObserver<BluetoothStatus>() {
            override fun onComplete() {
                Timber.d("onCompleted")
            }

            override fun onNext(status: BluetoothStatus?) {
                Timber.d("onNext: $status")
                bluetoothStatus.value = status
            }

            override fun onError(e: Throwable?) {
                Timber.e("onError: ${e?.localizedMessage}")
            }
        }, EmptyParam())
    }

}