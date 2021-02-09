package com.ceslab.firemesh.presentation.dialogs

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.node.NodeFragment
import kotlinx.android.synthetic.main.dialog_ota_config.view.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class OTADialogConfig : DialogFragment() {

    companion object {
        private const val FILE_CHOOSER_REQUEST_CODE = 9999

    }

    lateinit var mView: View
    private var isOTAInit: Boolean = false

    // OTA file paths
    private var appPath = ""
    private var currentMTU = 247
    private var currentPriority = 2
    private var isReliable = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        arguments?.let {
            if (it.containsKey(NodeFragment.IS_OTA_INIT_KEY)) {
                isOTAInit = it.getBoolean(NodeFragment.IS_OTA_INIT_KEY)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("onCreateView")
        val view = inflater.inflate(R.layout.dialog_ota_config, container, false)
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Make dialog concern rounded
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        setupViews(view)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                FILE_CHOOSER_REQUEST_CODE -> {
                    val uri = data?.data
                    val filename: String?

                    filename = try {
                        getFileName(uri)
                    } catch (e: Exception) {
                        ""
                    }

                    if (!hasOtaFileCorrectExtension(filename)) {
                        return
                    }
                    prepareOtaFile(uri, filename)
                }
            }
        }

        if (arePartialOTAFilesCorrect()) {
            mView.btn_ota_proceed.isClickable = true
            mView.btn_ota_proceed.setBackgroundColor(
                ContextCompat.getColor(
                    activity!!.applicationContext,
                    R.color.primary_color
                )
            )
        } else {
            mView.btn_ota_proceed.isClickable = false
            mView.btn_ota_proceed.setBackgroundColor(
                ContextCompat.getColor(
                    activity!!.applicationContext,
                    R.color.gray_7
                )
            )
        }
    }

    private fun setupViews(view: View) {
        Timber.d("setupViews")
        mView = view

        view.btn_ota_proceed.setBackgroundColor(
            ContextCompat.getColor(
                activity!!.applicationContext,
                R.color.gray_7
            )
        )
        view.btn_ota_proceed.setOnClickListener(onProceedButtonClicked)
        view.btn_select_application_gbl_file.setOnClickListener(onSelectApplicationFileClicked)
        view.btn_partial_ota.setOnClickListener(onPartialOTAButtonClicked)

        view.edt_mtu_value.setOnEditorActionListener(onMaxMTUValueEdited)

        view.seekbar_mtu.max = 250 - 23
        view.seekbar_mtu.progress = 250 - 23
        view.seekbar_mtu.setOnSeekBarChangeListener(onMTUBarChanged)

        view.seekbar_priority.max = 2
        view.seekbar_priority.progress = 2
        view.seekbar_priority.setOnSeekBarChangeListener(onPriorityBarChanged)

       view.rdb_reliability.setOnClickListener { isReliable = true }
        view.rdb_speed.setOnClickListener { isReliable = false }
    }

    private fun arePartialOTAFilesCorrect(): Boolean {
        Timber.d("arePartialOTAFilesCorrect")
        return mView.btn_select_application_gbl_file.text != "Select Application .gbl file"
    }

    private fun getFileName(uri: Uri?): String? {
        Timber.d("getFileName")
        var result: String? = null
        if ((uri?.scheme == "content")) {
            val cursor = activity!!.contentResolver.query(uri, null, null, null, null)
            cursor.use { c ->
                if (c != null && c.moveToFirst()) {
                    result = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri?.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }

    private fun prepareOtaFile(uri: Uri?, filename: String?) {
        Timber.d("prepareOtaFile")
        try {
            val inStream = activity!!.contentResolver.openInputStream(uri!!)
            if (inStream == null) {
                return
            }
            val file = File(activity!!.cacheDir, filename)
            val output: OutputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while ((inStream.read(buffer).also { read = it }) != -1) {
                output.write(buffer, 0, read)
            }

            appPath = file.absolutePath
            mView.btn_select_application_gbl_file.text = filename
            output.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun hasOtaFileCorrectExtension(filename: String?): Boolean {
        Timber.d("hasOtaFileCorrectExtension")
        return filename?.toUpperCase(Locale.getDefault())?.contains(".GBL")!!
    }

    private val onProceedButtonClicked = View.OnClickListener {
        Timber.d("onProceedButtonClicked")
    }


    private val onSelectApplicationFileClicked = View.OnClickListener {
        Timber.d("onSelectApplicationFileClicked")
        val intent = Intent().apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(
            Intent.createChooser(intent, "Choose directory"),
            FILE_CHOOSER_REQUEST_CODE
        )
    }

    private val onPartialOTAButtonClicked = View.OnClickListener {
        Timber.d("onPartialOTAButtonClicked")

    }

    private val onMaxMTUValueEdited = TextView.OnEditorActionListener { _, _, _ ->
        if (mView.edt_mtu_value.text != null) {
            var test = mView.edt_mtu_value.text.toString().toInt()
            if (test < 23) test = 23 else if (test > 250) test = 250
            mView.seekbar_mtu.progress = test - 23
            currentMTU = test
        }
        false
    }

    private val onMTUBarChanged = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            Timber.d("onProgressChanged: $progress")
            mView.edt_mtu_value.setText(" ${progress+23}")
            currentMTU = progress + 23
        }
    }

    private val onPriorityBarChanged = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            Timber.d("onProgressChanged: $progress")
            when (progress) {
                1 -> currentPriority = 0 //BALANCE
                2 -> currentPriority = 1 //HIGH
                0 -> currentPriority = 2 //LOW
            } //LOW
        }
    }

}