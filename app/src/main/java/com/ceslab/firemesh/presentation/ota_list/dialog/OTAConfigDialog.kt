package com.ceslab.firemesh.presentation.ota_list.dialog

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
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
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.factory.ViewModelFactory
import com.ceslab.firemesh.meshmodule.ota.OTAType
import com.ceslab.firemesh.presentation.node.NodeFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_ota_config.view.*
import timber.log.Timber
import javax.inject.Inject

class OTAConfigDialog : DialogFragment() {

    companion object {
        private const val FILE_CHOOSER_REQUEST_CODE = 9999

    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var otaConfigViewModel: OTAConfigViewModel
    private lateinit var mView: View

    private var isOTAInit: Boolean = false

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
        setupViewModel()
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

                    if (!otaConfigViewModel.hasOtaFileCorrectExtension(filename)) {
                        return
                    }
                    mView.btn_select_application_gbl_file.text = filename
                    otaConfigViewModel.prepareOtaFile(uri, filename)
                }
            }
        }

        if (arePartialOTAFilesCorrect()) {
            mView.btn_ota_proceed.isClickable = true
            mView.btn_ota_proceed.setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext, R.color.primary_color))
        } else {
            mView.btn_ota_proceed.isClickable = false
            mView.btn_ota_proceed.setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext, R.color.gray_7))
        }
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        otaConfigViewModel = ViewModelProvider(this, viewModelFactory).get(OTAConfigViewModel::class.java)
    }

    private fun setupViews(view: View) {
        Timber.d("setupViews")
        mView = view
        view.btn_ota_proceed.apply {
            setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext, R.color.gray_7))
            setOnClickListener(onProceedButtonClicked)
        }

        dialog!!.setCanceledOnTouchOutside(false)

        view.btn_ota_cancel.setOnClickListener(onCancelButtonClicked)

        view.btn_select_application_gbl_file.setOnClickListener(onSelectApplicationFileClicked)

        view.btn_partial_ota.apply {
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.button_clicked_color))
            setOnClickListener(onPartialOTAButtonClicked)
        }


        view.btn_full_ota.setOnClickListener(onFullOTAButtonClicked)

        view.edt_mtu_value.setOnEditorActionListener(onMaxMTUValueEdited)

        view.seekbar_mtu.apply {
            max = 250 - 23
            progress = 250 - 23
            setOnSeekBarChangeListener(onMTUBarChanged)
        }

        view.seekbar_priority.apply {
            max = 2
            progress = 2
            setOnSeekBarChangeListener(onPriorityBarChanged)
        }

        view.rdb_reliability.setOnClickListener { isReliable = true }

        view.rdb_speed.setOnClickListener { isReliable = false }
    }

    private fun changeOTATypeView(otaType: OTAType) {
        Timber.d("changeOTATypeView: $otaType")
        when (otaType) {
            OTAType.PARTIAL_OTA -> {
                mView.btn_partial_ota.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.button_clicked_color))
                mView.btn_full_ota.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.primary_color))
                mView.layout_app_loader.visibility = View.GONE
            }

            OTAType.FULL_OTA -> {
                mView.btn_full_ota.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.button_clicked_color))
                mView.btn_partial_ota.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.primary_color))
                mView.layout_app_loader.visibility = View.VISIBLE
            }
        }
    }


    private fun arePartialOTAFilesCorrect(): Boolean {
        Timber.d("arePartialOTAFilesCorrect")
        return mView.btn_select_application_gbl_file.text != "Select Application .gbl file"
    }

    private fun getFileName(uri: Uri?): String? {
        Timber.d("getFileName")
        var result: String? = null
        if ((uri?.scheme == "content")) {
            val cursor = context!!.contentResolver.query(uri, null, null, null, null)
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


    private val onProceedButtonClicked = View.OnClickListener {
        Timber.d("onProceedButtonClicked")
    //    otaConfigViewModel.startOTAProcess(isReliable,currentMTU,currentPriority)
        mView.layout_setup.visibility = View.GONE
        mView.layout_progress.visibility = View.VISIBLE
    }

    private val onCancelButtonClicked = View.OnClickListener {
        Timber.d("onCancelButtonClicked")
        dialog!!.dismiss()
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
        changeOTATypeView(OTAType.PARTIAL_OTA)
        otaConfigViewModel.setOTAType(OTAType.PARTIAL_OTA)
    }

    private val onFullOTAButtonClicked = View.OnClickListener {
        Timber.d("onFullOTAButtonClicked")
         changeOTATypeView(OTAType.FULL_OTA)
        //otaMode = OTAMode.FULL_OTA

    }

    private val onMaxMTUValueEdited = TextView.OnEditorActionListener { _, _, _ ->
        if (mView.edt_mtu_value.text != null) {
            var mtuValue = mView.edt_mtu_value.text.toString().toInt()
            if (mtuValue < 23) mtuValue = 23 else if (mtuValue > 250) mtuValue = 250
            mView.seekbar_mtu.progress = mtuValue - 23
            currentMTU = mtuValue
        }
        false
    }

    private val onMTUBarChanged = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            Timber.d("onProgressChanged: $progress")
            mView.edt_mtu_value.setText(" ${progress + 23}")
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