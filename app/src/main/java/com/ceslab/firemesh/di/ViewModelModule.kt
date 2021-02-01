package com.ceslab.firemesh.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.factory.ViewModelFactory
import com.ceslab.firemesh.factory.ViewModelKey
import com.ceslab.firemesh.presentation.main.activity.MainActivityViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun bindViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(MainActivityViewModel::class)
    abstract fun bindMainActivityModel(mainActivityViewModel: MainActivityViewModel): ViewModel
}