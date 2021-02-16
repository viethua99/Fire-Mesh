package com.ceslab.firemesh.presentation.ota_list.dialog.ota_config_dialog

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.ceslab.firemesh.meshmodule.ota.OTAType
import com.ceslab.firemesh.meshmodule.ota.OTAManager
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import javax.inject.Inject

class OTAConfigViewModel @Inject constructor(
    private val context: Context,
    private val otaManager: OTAManager
) : ViewModel() {

    // OTA file paths
    private var applicationPath = ""
    private var stackPath = ""

    private var otaType = OTAType.PARTIAL_OTA


    fun setOTAType(type: OTAType) {
        this.otaType = type
    }

    fun hasOtaFileCorrectExtension(filename: String?): Boolean {
        Timber.d("hasOtaFileCorrectExtension")
        return filename?.toUpperCase(Locale.getDefault())?.contains(".GBL")!!
    }

    fun prepareOtaFile(uri: Uri?, name: String?) {
        Timber.d("prepareOtaFile")
        try {
            val inStream = context.contentResolver.openInputStream(uri!!) ?: return
            val file = File(context.cacheDir, name)
            val output: OutputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while ((inStream.read(buffer).also { read = it }) != -1) {
                output.write(buffer, 0, read)
            }
            applicationPath = file.absolutePath
            output.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun startOTAProcess(isReliable:Boolean,mtuValue:Int,priority:Int) {
        Timber.d("startOTAProcess: $isReliable -- $mtuValue -- $priority")
        otaManager.startOTAProcess(isReliable,mtuValue,priority,stackPath,applicationPath)
    }

}