package com.ceslab.firemesh.di

import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.factory.ViewModelFactory
import dagger.Binds
import dagger.Module

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun bindViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory

}