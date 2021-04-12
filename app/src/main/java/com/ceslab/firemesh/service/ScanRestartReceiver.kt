package com.ceslab.firemesh.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import timber.log.Timber

/**
 * Created by Viet Hua on 04/01/2021.
 */


class ScanRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("Broadcast Listened\", \"Service tried to stop")
        Toast.makeText(context,"Service Restarted",Toast.LENGTH_SHORT).show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.d("Larger")
            context.startForegroundService(Intent(context,FireMeshService::class.java))
        } else {
            Timber.d("Smaller")
            context.startService(Intent(context,FireMeshService::class.java))
        }
    }

}