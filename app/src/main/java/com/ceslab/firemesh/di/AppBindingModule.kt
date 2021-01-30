package com.ceslab.firemesh.di

import com.ceslab.firemesh.presentation.main.MainActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [ViewModelModule::class])
abstract class AppBindingModule {
    @ContributesAndroidInjector
    abstract fun mainActivity() : MainActivity

}