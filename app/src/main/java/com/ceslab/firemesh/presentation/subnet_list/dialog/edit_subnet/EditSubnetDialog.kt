package com.ceslab.firemesh.presentation.subnet_list.dialog.edit_subnet

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import cn.pedant.SweetAlert.SweetAlertDialog
import com.ceslab.firemesh.R
import com.ceslab.firemesh.factory.ViewModelFactory
import com.ceslab.firemesh.meshmodule.listener.ConnectionMessageListener
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.ceslab.firemesh.util.AndroidDialogUtil
import com.ceslab.firemesh.util.AppUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_edit_subnet_bottom_sheet.view.*
import kotlinx.android.synthetic.main.fragment_subnet.*
import timber.log.Timber
import javax.inject.Inject

class EditSubnetDialog(private val subnet: Subnet) : BottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private lateinit var editSubnetCallback: EditSubnetCallback
    private lateinit var editSubnetViewModel: EditSubnetViewModel

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
        val view = inflater.inflate(R.layout.dialog_edit_subnet_bottom_sheet, container, false)
        view.btn_delete_subnet.setOnClickListener(onDeleteSubnetButtonClicked)
        view.btn_save_changes_subnet.setOnClickListener(onSaveChangesSubnetButtonClicked)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        setupViewModel()
        checkSubnetNameEditTextChanged(view)

        dialog?.setOnShowListener {
            val dialog = it as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                sheet.parent.parent.requestLayout()
            }
        }
    }

    fun setEditSubnetCallback(editSubnetCallback: EditSubnetCallback) {
        this.editSubnetCallback = editSubnetCallback
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        editSubnetViewModel =
            ViewModelProvider(this, viewModelFactory).get(EditSubnetViewModel::class.java)
        editSubnetViewModel.apply {
            getRemoveSubnetStatus().observe(this@EditSubnetDialog, removeSubnetObserver)
            getMeshStatus().observe(this@EditSubnetDialog, meshStatusObserver)
            getConnectionMessage().observe(this@EditSubnetDialog, connectionMessageObserver)
        }
    }

    private fun checkSubnetNameEditTextChanged(view:View){
        view.btn_save_changes_subnet.isClickable = false
        view.btn_save_changes_subnet.setBackgroundColor(
            ContextCompat.getColor(
                activity!!.applicationContext,
                R.color.gray_7
            )
        )

        view.edt_subnet_name.setText(subnet.name)
        view.edt_subnet_name.doOnTextChanged { text, _, _, _ ->
            if (text!!.isEmpty() || text.toString() == subnet.name) {
                view.btn_save_changes_subnet.isClickable = false
                view.btn_save_changes_subnet.setBackgroundColor(
                    ContextCompat.getColor(
                        activity!!.applicationContext,
                        R.color.gray_7
                    )
                )
            } else {
                view.btn_save_changes_subnet.isClickable = true
                view.btn_save_changes_subnet.setBackgroundColor(
                    ContextCompat.getColor(
                        activity!!.applicationContext,
                        R.color.primary_color
                    )
                )
            }
        }
    }

      private fun showDeleteSubnetLocallyDialog() {
        activity?.runOnUiThread {
            AndroidDialogUtil.getInstance().hideDialog()
            SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Delete locally?")
                .setContentText("Delete failed, do you want to delete subnet from the app?")
                .setConfirmText("Delete")
                .showCancelButton(false)
                .setConfirmClickListener {
                    editSubnetViewModel.deleteSubnetLocally(subnet)
                    editSubnetCallback.onChanged()
                    it.cancel()
                    dismiss()
                }
                .show()
        }
    }

    private val onDeleteSubnetButtonClicked = View.OnClickListener {
        activity?.runOnUiThread {
            AndroidDialogUtil.getInstance().showLoadingDialog(activity, "Removing subnet")
            editSubnetViewModel.removeSubnet(subnet)

        }
    }

    private val onSaveChangesSubnetButtonClicked = View.OnClickListener {
        val newSubnetName = view!!.edt_subnet_name.text.toString()
        editSubnetViewModel.updateSubnet(subnet, newSubnetName)
        editSubnetCallback.onChanged()
        dialog!!.dismiss()
    }

    private val removeSubnetObserver = Observer<Boolean> {
        editSubnetCallback.onChanged()
        activity?.runOnUiThread {
            dialog!!.dismiss()
            if (it == true) {
                AndroidDialogUtil.getInstance().showSuccessDialog(activity, "Remove subnet succeed")
            } else {
                showDeleteSubnetLocallyDialog()
            }
        }
    }
    private val meshStatusObserver = Observer<MeshStatus> { meshStatus ->
        Timber.d("meshStatusObserver = $meshStatus")
        activity?.runOnUiThread {
            tv_subnet_connection.apply {
                when (meshStatus) {
                    MeshStatus.MESH_CONNECTING -> {
                        AndroidDialogUtil.getInstance()
                            .showLoadingDialog(activity!!, "Connecting to subnet")
                    }

                }
            }

        }
    }
    private val connectionMessageObserver = Observer<ConnectionMessageListener.MessageType> {
        activity?.runOnUiThread {
            when (it) {
                ConnectionMessageListener.MessageType.CONNECTING_TO_SUBNET_ERROR -> AndroidDialogUtil.getInstance().showWarningDialog(activity!!,"Connecting to subnet error")
                ConnectionMessageListener.MessageType.REMOVING_SUBNET -> AndroidDialogUtil.getInstance().showLoadingDialog(activity!!,"Removing subnet")
            }
        }
    }



}