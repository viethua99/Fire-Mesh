package com.ceslab.domain.base

import com.ceslab.domain.executor.ExecutionThread
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.MaybeObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.DisposableMaybeObserver

abstract class MaybeUseCase<T> constructor(private val executionThread: ExecutionThread) {


    private var compositeDisposable = CompositeDisposable()

    fun execute(disposableMaybeObserver: DisposableMaybeObserver<*>, t: T) {
        val maybe = buildUseCase(t)
            .subscribeOn(executionThread.io())
            .observeOn(executionThread.main())
        addDisposable(maybe.subscribeWith(disposableMaybeObserver as MaybeObserver<Any>) as Disposable)
    }

    fun dispose() {
        compositeDisposable.dispose()
    }


    protected abstract fun buildUseCase(t: T): Maybe<*>

    private fun addDisposable(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }


}