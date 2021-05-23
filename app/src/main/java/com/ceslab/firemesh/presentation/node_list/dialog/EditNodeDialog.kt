package com.ceslab.firemesh.presentation.node_list.dialog

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import cn.pedant.SweetAlert.SweetAlertDialog
import com.ceslab.firemesh.R
import com.ceslab.firemesh.factory.ViewModelFactory
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.util.AndroidDialogUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_delete_node_bottom_sheet.view.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Viet Hua on 03/02/2021.
 */

class EditNodeDialog(private val meshNode: MeshNode) : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var editNodeViewModel: EditNodeDialogViewModel
    private lateinit var editNodeCallback: EditNodeCallback

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
        val view = inflater.inflate(R.layout.dialog_delete_node_bottom_sheet, container, false)
        view.btn_delete_node.setOnClickListener(onDeleteNodeButtonClicked)
        view.btn_save_changes_node.setOnClickListener(onSaveChangesNodeButtonClicked)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        setupViewModel()
        checkNodeNameEditTextChanged(view)
        dialog?.setOnShowListener {
            val dialog = it as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                sheet.parent.parent.requestLayout()
            }
        }
    }

    fun setDeleteNodeCallback(editNodeCallback: EditNodeCallback) {
        this.editNodeCallback = editNodeCallback
    }


    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        editNodeViewModel =
            ViewModelProvider(this, viewModelFactory).get(EditNodeDialogViewModel::class.java)
        editNodeViewModel.getDeleteNodeStatus().observe(this, isNodeDeletedSucceedObserver)

    }

    private fun checkNodeNameEditTextChanged(view: View) {
        view.btn_save_changes_node.isClickable = false
        view.btn_save_changes_node.setBackgroundColor(
            ContextCompat.getColor(
                activity!!.applicationContext,
                R.color.gray_7
            )
        )

        view.edt_node_name.setText(meshNode.node.name)
        view.edt_node_name.doOnTextChanged { text, _, _, _ ->
            if (text!!.isEmpty() || text.toString() == meshNode.node.name) {
                view.btn_save_changes_node.isClickable = false
                view.btn_save_changes_node.setBackgroundColor(
                    ContextCompat.getColor(
                        activity!!.applicationContext,
                        R.color.gray_7
                    )
                )
            } else {
                view.btn_save_changes_node.isClickable = true
                view.btn_save_changes_node.setBackgroundColor(
                    ContextCompat.getColor(
                        activity!!.applicationContext,
                        R.color.primary_color
                    )
                )
            }
        }
    }

    private fun showDeleteDeviceLocallyDialog() {
        activity?.runOnUiThread {
            SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Delete locally?")
                .setContentText("Delete failed, do you want to delete node locally?")
                .setConfirmText("Delete")
                .showCancelButton(false)
                .setConfirmClickListener {
                    editNodeViewModel.deleteDeviceLocally(meshNode.node)
                    editNodeCallback.onChanged()
                    it.cancel()
                    dismiss()
                }
                .show()
        }
    }


    private val onDeleteNodeButtonClicked = View.OnClickListener {
        AndroidDialogUtil.getInstance().showLoadingDialog(activity, "Deleting node")
        editNodeViewModel.deleteNode(meshNode)
    }

    private val onSaveChangesNodeButtonClicked = View.OnClickListener {
        val newNodeName = view!!.edt_node_name.text.toString()
        editNodeViewModel.updateNode(meshNode, newNodeName)
        editNodeCallback.onChanged()
        dialog!!.dismiss()
    }

    private val isNodeDeletedSucceedObserver = Observer<Boolean> {
        activity?.runOnUiThread {
            AndroidDialogUtil.getInstance().hideDialog()
            if (it == true) {
                dialog!!.dismiss()
                AndroidDialogUtil.getInstance().showSuccessDialog(activity, "Delete node succeed")
                editNodeCallback.onChanged()
            } else {
                showDeleteDeviceLocallyDialog()
            }
        }
    }

}