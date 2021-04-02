package com.ceslab.firemesh.di

import com.ceslab.firemesh.background_service.FireMeshService
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module

abstract class ServiceModule {
    @ContributesAndroidInjector
    abstract fun fireMeshService() : FireMeshService
}