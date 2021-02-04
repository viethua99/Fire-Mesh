package com.ceslab.firemesh.util

import timber.log.Timber

class AppUtil {
    companion object {
        fun isNameValid(name: String): Boolean {
            Timber.d("isNameValid: $name")
            if (name.trim().isNotEmpty()) {
                return true
            }
            return false
        }
    }
}