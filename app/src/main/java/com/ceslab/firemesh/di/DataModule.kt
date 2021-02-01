package com.ceslab.firemesh.di

import com.ceslab.data.BluetoothRepositoryImpl
import com.ceslab.domain.repository.BluetoothRepository
import dagger.Binds
import dagger.Module

@Module
abstract class DataModule {
    @Binds
    abstract fun bindBluetoothRepository(bluetoothRepositoryImpl: BluetoothRepositoryImpl): BluetoothRepository
}