package com.ceslab.domain.base

import com.ceslab.domain.executor.ExecutionThread
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.MaybeObserver
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.DisposableObserver

abstract class ObservableUseCase<T> constructor(private val executionThread: ExecutionThread) {


    private var compositeDisposable = CompositeDisposable()

    fun execute(disposableObserver: DisposableObserver<*>, t: T) {
        val observable = buildUseCase(t)
            .subscribeOn(executionThread.io())
            .observeOn(executionThread.main())
        addDisposable(observable.subscribeWith(disposableObserver as Observer<Any>) as Disposable)
    }

    fun dispose() {
        compositeDisposable.dispose()
    }


    protected abstract fun buildUseCase(t: T): Observable<*>

    private fun addDisposable(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }


}