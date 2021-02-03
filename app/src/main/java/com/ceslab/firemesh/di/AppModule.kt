package com.ceslab.firemesh.di

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import com.ceslab.firemesh.meshmodule.bluetoothle.BluetoothScanner
import com.ceslab.firemesh.meshmodule.bluetoothle.BluetoothStateReceiver
import com.ceslab.firemesh.meshmodule.bluetoothle.LocationStateReceiver
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

    @Provides
    @Singleton
    fun provideBluetoothScanner(bluetoothStateReceiver: BluetoothStateReceiver): BluetoothScanner {
        return BluetoothScanner(bluetoothStateReceiver)
    }

}