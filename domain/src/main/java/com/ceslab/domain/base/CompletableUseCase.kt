package com.ceslab.domain.base

import com.ceslab.domain.executor.ExecutionThread
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.DisposableCompletableObserver

abstract class CompletableUseCase<T> constructor(private val executionThread: ExecutionThread) {


    private var compositeDisposable = CompositeDisposable()


    fun execute(disposableMaybeObserver: DisposableCompletableObserver, t: T) {
        val maybe = buildUseCase(t)
            .subscribeOn(executionThread.io())
            .observeOn(executionThread.main())
        addDisposable(maybe.subscribeWith(disposableMaybeObserver as CompletableObserver) as Disposable)
    }

    fun dispose() {
        compositeDisposable.dispose()
    }


    protected abstract fun buildUseCase(t: T): Completable

    private fun addDisposable(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }


}
