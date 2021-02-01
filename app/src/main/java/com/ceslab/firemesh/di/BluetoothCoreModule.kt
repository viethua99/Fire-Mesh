package com.ceslab.firemesh.di

import com.ceslab.ble_mesh_core.BluetoothCoreImpl
import com.ceslab.data.BluetoothRepositoryImpl
import com.ceslab.data.core.BluetoothCore
import com.ceslab.domain.repository.BluetoothRepository
import dagger.Binds
import dagger.Module

@Module
abstract class BluetoothCoreModule {
    @Binds
    abstract fun bindBluetoothCore(bluetoothCoreImpl: BluetoothCoreImpl): BluetoothCore
}