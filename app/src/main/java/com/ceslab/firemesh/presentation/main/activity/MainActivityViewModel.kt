package com.ceslab.firemesh.presentation.main.activity

import android.os.Handler
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ceslab.domain.interactor.TestUseCase
import io.reactivex.rxjava3.observers.DisposableCompletableObserver
import javax.inject.Inject

class MainActivityViewModel @Inject constructor() : ViewModel() {
    val status = MutableLiveData<Boolean>()

    @Inject
    lateinit var testUseCase: TestUseCase

    fun test() {
        testUseCase.execute(object : DisposableCompletableObserver(){
            override fun onComplete() {
                status.value = true
            }

            override fun onError(e: Throwable?) {
            }
        },"Test")
    }
}