package com.ceslab.firemesh.di

import android.content.Context
import com.ceslab.domain.executor.ExecutionThread
import com.ceslab.firemesh.myapp.MyApplication
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {
    @Provides
    @Singleton
    fun provideContext(myApplication: MyApplication):Context {
        return myApplication
    }

    @Provides
    fun provideExecutionThread(): ExecutionThread {
        return ExecutionThreadImpl()
    }

}