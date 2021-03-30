package com.ceslab.firemesh.presentation.node.node_config.unbind_dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.factory.ViewModelFactory
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.ceslab.firemesh.util.AndroidDialogUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_model_unbind_bottom_sheet.*
import kotlinx.android.synthetic.main.dialog_model_unbind_bottom_sheet.view.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Viet Hua on 03/30/2021.
 */

class ModelUnbindDialog(private val vendorFunctionality: NodeFunctionality.FunctionalityNamed) : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var modelUnbindViewModel: ModelUnbindViewModel

    private lateinit var modelUnbindCallback: ModelUnbindCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("onCreateView")
        val view = inflater.inflate(R.layout.dialog_model_unbind_bottom_sheet, container, false)
        view.btn_unbind_model.setOnClickListener(onUnbindButtonClicked)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        setupViewModel()
        tv_functionality_name.setText(vendorFunctionality.functionalityName)
        dialog?.setOnShowListener {
            val dialog = it as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                sheet.parent.parent.requestLayout()
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Timber.d("onCancel")
        modelUnbindCallback.onCancel()
    }

    fun setModelUnbindCallback(unbindCallback: ModelUnbindCallback) {
        this.modelUnbindCallback = unbindCallback
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        modelUnbindViewModel = ViewModelProvider(this, viewModelFactory).get(ModelUnbindViewModel::class.java)
        modelUnbindViewModel.getConfigFinishStatus().observe(this,onConfigFinishedObserver)
    }




    private val onUnbindButtonClicked = View.OnClickListener {
        Timber.d("onStartConfigButtonClicked: functionality=$vendorFunctionality")
        AndroidDialogUtil.getInstance().showLoadingDialog(activity, "Starting Config Model")
        modelUnbindViewModel.unbindFunctionality(vendorFunctionality.functionality)
    }

    private val onConfigFinishedObserver = Observer<Boolean> {
        Timber.d("onConfigFinished")
        AndroidDialogUtil.getInstance().hideDialog()
        dialog!!.dismiss()
    }


}