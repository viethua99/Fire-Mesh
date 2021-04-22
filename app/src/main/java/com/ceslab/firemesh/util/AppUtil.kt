package com.ceslab.firemesh.util

import android.content.Context
import com.ceslab.firemesh.R
import com.siliconlab.bluetoothmesh.adk.ErrorType
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

        fun convertErrorMessage(context: Context, errorType: ErrorType): String {
            val errorCode = errorType.errorCode
            val errorContent = convertErrorType(context, errorType.type)
                ?.plus(errorType.message?.takeUnless { it.contains(errorCode.toString()) }
                    ?.let { ": $it" } ?: "")

            return if (errorCode != null && errorContent != null) {
                context.getString(R.string.error_message_code_and_content, errorCode, errorContent)
            } else if (errorCode != null && errorContent == null) {
                context.getString(R.string.error_message_code, errorCode)
            } else if (errorCode == null && errorContent != null) {
                errorContent
            } else {
                context.getString(R.string.error_message_unknown)
            }
        }

        private fun convertErrorType(context: Context, errorType: ErrorType.TYPE?): String? {
            if (errorType == null) {
                return null
            }

            return when (errorType) {
                ErrorType.TYPE.TIMEOUT -> context.getString(R.string.error_message_timeout)
                ErrorType.TYPE.UNKNOWN -> context.getString(R.string.error_message_unknown)
                ErrorType.TYPE.API_ERROR -> context.getString(R.string.error_message_api_error)
                ErrorType.TYPE.COULD_NOT_CONNECT_TO_DEVICE -> context.getString(R.string.error_message_could_not_connect_to_device)
                ErrorType.TYPE.COULD_NOT_SUBSCRIBE_TO_CHARACTERISTIC -> context.getString(R.string.error_message_could_not_subscribe_to_characteristic)
                ErrorType.TYPE.COULD_NOT_WRITE_TO_CHARACTERISTIC -> context.getString(R.string.error_message_could_write_to_characteristic)
                ErrorType.TYPE.COULD_NOT_DISCONNECT_FROM_DEVICE -> context.getString(R.string.error_message_could_not_disconnect_from_device)
                ErrorType.TYPE.COULD_NOT_READ_SERVICE_DATA -> context.getString(R.string.error_message_could_not_read_service_data)
                ErrorType.TYPE.DISCONNECTED_FROM_DEVICE -> context.getString(R.string.error_message_disconnected_from_device)
                ErrorType.TYPE.PROVISIONING_ALREADY_PENDING -> context.getString(R.string.error_message_provisioning_already_pending)
                ErrorType.TYPE.MAX_CONNECTIONS_ACTIVE -> context.getString(R.string.error_message_max_connections_active)
                ErrorType.TYPE.COULD_NOT_PROVISION -> context.getString(R.string.error_message_could_not_provision)
                ErrorType.TYPE.CANNOT_SAVE_TO_DATABASE -> context.getString(R.string.error_message_cannot_save_to_database)
                else -> context.getString(R.string.error_message_unknown)
            }
        }
    }
}