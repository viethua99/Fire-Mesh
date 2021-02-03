package com.ceslab.firemesh.util

import timber.log.Timber

class AppUtil {
    companion object {
        fun isDeviceNameValid(deviceName: String): Boolean {
            Timber.d("isDeviceNameValid: $deviceName")
            if (deviceName.trim().isNotEmpty()) {
                return true
            }
            return false
        }
    }
}