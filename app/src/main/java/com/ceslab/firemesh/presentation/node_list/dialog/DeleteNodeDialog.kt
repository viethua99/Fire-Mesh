package com.ceslab.firemesh.presentation.node_list.dialog

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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

class DeleteNodeDialog(private val meshNode: MeshNode) : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var deleteNodeViewModel: DeleteNodeDialogViewModel
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
        val view = inflater.inflate(R.layout.dialog_delete_node_bottom_sheet, container, false)
        view.btn_delete_node.setOnClickListener(onDeleteNodeButtonClicked)
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
            val builder = AlertDialog.Builder(activity, R.style.AlertDialogTheme)
            builder.apply {
                setTitle("Delete Locally")
                setMessage("Delete failed , Do you want to delete node locally?")
                setPositiveButton("Delete") { dialog, _ ->
                    deleteNodeViewModel.deleteDeviceLocally(meshNode.node)
                    deleteNodeCallback.onChanged()
                    dialog.dismiss()
                    dismiss()
                }

                setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    dismiss()
                }
            }

            builder.create().show()
        }
    }


    private val onDeleteNodeButtonClicked = View.OnClickListener {
        AndroidDialogUtil.getInstance().showLoadingDialog(activity, "Deleting Node")
        deleteNodeViewModel.deleteNode(meshNode)
    }

    private val isNodeDeletedSucceedObserver = Observer<Boolean> {
        activity?.runOnUiThread {
            AndroidDialogUtil.getInstance().hideDialog()
            if (it == true) {
                dialog!!.dismiss()
                AndroidDialogUtil.getInstance().showSuccessDialog(activity, "Delete Node Succeed")
                deleteNodeCallback.onChanged()
            } else {
                showDeleteDeviceLocallyDialog()
            }
        }
    }

}