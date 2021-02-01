package com.ceslab.firemesh.di

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import com.ceslab.ble_mesh_core.BluetoothCoreImpl
import com.ceslab.ble_mesh_core.bluetoothle.BluetoothStateReceiver
import com.ceslab.ble_mesh_core.bluetoothle.LocationStateReceiver
import com.ceslab.data.BluetoothRepositoryImpl
import com.ceslab.data.core.BluetoothCore
import com.ceslab.domain.repository.BluetoothRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
 class BluetoothCoreModule {
    @Provides
    @Singleton
    fun bindBluetoothCore(bluetoothCoreImpl: BluetoothCoreImpl): BluetoothCore {
        return bluetoothCoreImpl
    }

    @Provides
    @Singleton
    fun provideBluetoothStateReceiver(context: Context): BluetoothStateReceiver {
        val bluetoothStateReceiver = BluetoothStateReceiver()
        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, intentFilter)
        return bluetoothStateReceiver
    }

    @Provides
    @Singleton
    fun provideLocationStateReceiver(context: Context): LocationStateReceiver {
        val locationStateReceiver = LocationStateReceiver()
        val intentFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(locationStateReceiver, intentFilter)
        return locationStateReceiver
    }
}