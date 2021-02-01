package com.ceslab.firemesh.di

import com.ceslab.firemesh.myapp.MyApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import javax.inject.Singleton

@Singleton
@Component(modules = [AndroidInjectionModule::class, AppModule::class, AppBindingModule::class, DataModule::class, BluetoothCoreModule::class])
interface AppComponent : AndroidInjector<DaggerApplication> {
    fun inject(myApplication: MyApplication)

    @Component.Builder
    interface Builder {
        fun build(): AppComponent

        @BindsInstance
        fun application(myApplication: MyApplication): Builder

    }

}