package com.ceslab.firemesh.meshmodule.bluetoothle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log
import timber.log.Timber

/**
 * Created by Viet Hua on 11/23/2020.
 */

class LocationStateReceiver : BroadcastReceiver() {

    private val listeners: ArrayList<LocationStateListener> = ArrayList()

    fun addListener(locationStateListener: LocationStateListener) {
        synchronized(listeners) {
            listeners.add(locationStateListener)
        }
    }

    fun removeListener(locationStateListener: LocationStateListener) {
        synchronized(listeners) {
            listeners.remove(locationStateListener)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
            notifyListeners()
        }
    }

    private fun notifyListeners() {
        synchronized(listeners) {
            listeners.forEach { listener ->
                Timber.d("onLocationStateChanged")
                listener.onLocationStateChanged()
            }
        }
    }

    interface LocationStateListener {
        fun onLocationStateChanged()
    }
}