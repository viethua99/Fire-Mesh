package com.ceslab.firemesh.presentation.node.node_config.config_dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.CompoundButtonCompat
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
import kotlinx.android.synthetic.main.dialog_model_config_bottom_sheet.*
import kotlinx.android.synthetic.main.dialog_model_config_bottom_sheet.view.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Viet Hua on 03/09/2021.
 */

class ModelConfigDialog(private val vendorFunctionality: NodeFunctionality.FunctionalityNamed) :
    BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var modelConfigViewModel: ModelConfigViewModel

    private lateinit var modelConfigCallback: ModelConfigCallback

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
        val view = inflater.inflate(R.layout.dialog_model_config_bottom_sheet, container, false)
        view.btn_start_config.setOnClickListener(onStartConfigButtonClicked)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        setupViewModel()
        tv_functionality_name.setText(vendorFunctionality.functionalityName)
        if (vendorFunctionality.functionality.isSupportPublication) {
            cb_set_publication.isEnabled = true
            cb_set_publication.isChecked = true
        } else {
            CompoundButtonCompat.setButtonTintList(cb_set_publication,ContextCompat.getColorStateList(context!!, R.color.gray_7))
            cb_set_publication.isEnabled = false
            cb_set_publication.isChecked = false
        }

        if (vendorFunctionality.functionality.isSupportSubscription) {
            cb_add_subscription.isEnabled = true
            cb_add_subscription.isChecked = true
        } else {
            CompoundButtonCompat.setButtonTintList(cb_add_subscription,ContextCompat.getColorStateList(context!!, R.color.gray_7))
            cb_add_subscription.isEnabled = false
            cb_add_subscription.isChecked = false
        }

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
        modelConfigCallback.onCancel()
    }

    fun setModelConfigCallback(modelConfigCallback: ModelConfigCallback) {
        this.modelConfigCallback = modelConfigCallback
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        modelConfigViewModel =
            ViewModelProvider(this, viewModelFactory).get(ModelConfigViewModel::class.java)
        modelConfigViewModel.getConfigFinishStatus().observe(this, onConfigFinishedObserver)
    }


    private val onStartConfigButtonClicked = View.OnClickListener {
        val isSetPublication = cb_set_publication.isChecked
        val isAddSubscription = cb_add_subscription.isChecked
        Timber.d("onStartConfigButtonClicked: functionality=$vendorFunctionality -- publication=$isSetPublication --subscription=$isAddSubscription")
        AndroidDialogUtil.getInstance().showLoadingDialog(activity, "Starting Config Model")
        modelConfigViewModel.bindFunctionality(
            vendorFunctionality.functionality,
            isSetPublication,
            isAddSubscription
        )
    }

    private val onConfigFinishedObserver = Observer<Boolean> {
        Timber.d("onConfigFinished")
        AndroidDialogUtil.getInstance().hideDialog()
        dialog!!.dismiss()
    }


}