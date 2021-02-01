package com.ceslab.domain.executor

import io.reactivex.rxjava3.core.Scheduler

interface ExecutionThread {
    fun main() : Scheduler

    fun io() : Scheduler


}