package com.ceslab.firemesh.presentation.node.node_config.dialog

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.factory.ViewModelFactory
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.presentation.node_list.dialog.DeleteNodeCallback
import com.ceslab.firemesh.presentation.node_list.dialog.DeleteNodeDialogViewModel
import com.ceslab.firemesh.util.AndroidDialogUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_delete_node_bottom_sheet.view.*
import kotlinx.android.synthetic.main.dialog_model_config_bottom_sheet.view.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Viet Hua on 03/09/2021.
 */

class ModelConfigDialog(private val meshNode: MeshNode) : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var deleteNodeCallback: DeleteNodeCallback

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
        view.btn_start_config.setOnClickListener(onDeleteNodeButtonClicked)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        setupViewModel()
        dialog?.setOnShowListener {
            val dialog = it as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                sheet.parent.parent.requestLayout()
            }
        }
    }

    fun setDeleteNodeCallback(deleteNodeCallback: DeleteNodeCallback) {
        this.deleteNodeCallback = deleteNodeCallback
    }


    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        deleteNodeViewModel =
            ViewModelProvider(this, viewModelFactory).get(DeleteNodeDialogViewModel::class.java)
        deleteNodeViewModel.getDeleteNodeStatus().observe(this, isNodeDeletedSucceedObserver)

    }

    private fun showDeleteDeviceLocallyDialog() {
        activity?.runOnUiThread {
            val builder = AlertDialog.Builder(activity, R.style.Theme_AppCompat_Light_Dialog_Alert)
            builder.apply {
                setTitle("Delete Locally")
                setMessage("Delete failed , ¬Do you want to delete locally")
                setPositiveButton("Delete") { dialog, _ ->
                    deleteNodeViewModel.deleteDeviceLocally(meshNode.node)
                    deleteNodeCallback.onChanged()
                    dialog.dismiss()
                }

                setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
            }

            builder.create().show()
        }
    }


    private val onStartConfigButtonClicked = View.OnClickListener {
        AndroidDialogUtil.getInstance().showLoadingDialog(activity, "")
        deleteNodeViewModel.deleteNode(meshNode)
    }

    private val isNodeDeletedSucceedObserver = Observer<Boolean> {
        activity?.runOnUiThread {
            AndroidDialogUtil.getInstance().hideDialog()
            if (it == true) {
                dialog!!.dismiss()
                Toast.makeText(activity!!, "Delete Node Succeed", Toast.LENGTH_SHORT).show()
                deleteNodeCallback.onChanged()
            } else {
                showDeleteDeviceLocallyDialog()
            }
        }
    }

}