package com.ceslab.firemesh.presentation.group_list.dialog.edit_group

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.factory.ViewModelFactory
import com.ceslab.firemesh.presentation.subnet_list.dialog.edit_subnet.EditSubnetViewModel
import com.ceslab.firemesh.util.AndroidDialogUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_edit_group_bottom_sheet.view.*
import timber.log.Timber
import javax.inject.Inject

class EditGroupDialog(private val group: Group) : BottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private lateinit var editGroupCallback: EditGroupCallback
    private lateinit var editGroupViewModel: EditGroupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Timber.d("onCreateView")
        val view = inflater.inflate(R.layout.dialog_edit_group_bottom_sheet, container, false)
        view.btn_delete_group.setOnClickListener(onDeleteGroupButtonClicked)
        view.btn_save_changes_group.setOnClickListener(onSaveChangesGroupButtonClicked)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        setupViewModel()

        view.btn_save_changes_group.isClickable = false
        view.btn_save_changes_group.setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext, R.color.gray_7))

        view.edt_group_name.setText(group.name)
        view.edt_group_name.doOnTextChanged { text, _, _, _ ->
            if(text!!.isEmpty() || text.toString() == group.name) {
                view.btn_save_changes_group.isClickable = false
                view.btn_save_changes_group.setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext, R.color.gray_7))
            } else {
                view.btn_save_changes_group.isClickable = true
                view.btn_save_changes_group.setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext, R.color.primary_color))
            }
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

    fun setEditGroupCallback(editGroupCallback: EditGroupCallback){
        this.editGroupCallback = editGroupCallback
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        editGroupViewModel = ViewModelProvider(this, viewModelFactory).get(EditGroupViewModel::class.java)
        editGroupViewModel.getRemoveGroupStatus().observe(this,removeGroupObserver)

    }

    private fun showDeleteGroupLocallyDialog() {
        activity?.runOnUiThread {
            val builder = AlertDialog.Builder(activity, R.style.Theme_AppCompat_Light_Dialog_Alert)
            builder.apply {
                setTitle("Delete Locally")
                setMessage("Delete failed,Do you want to delete group from the app?")
                setPositiveButton("Delete") { dialog, _ ->
                    editGroupViewModel.removeGroupLocally(group)
                    editGroupCallback.onChanged()
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

    private val onDeleteGroupButtonClicked = View.OnClickListener {
        AndroidDialogUtil.getInstance().showLoadingDialog(activity, "Removing Group")
        editGroupViewModel.removeGroup(group)
    }

    private val onSaveChangesGroupButtonClicked = View.OnClickListener {
        val newGroupName = view!!.edt_group_name.text.toString()
        editGroupViewModel.updateGroup(group,newGroupName)
        editGroupCallback.onChanged()
        dialog!!.dismiss()
    }

    private val removeGroupObserver = Observer<Boolean> {
        editGroupCallback.onChanged()
        activity?.runOnUiThread {
            dialog!!.dismiss()
            if(it == true) {
                AndroidDialogUtil.getInstance().showSuccessDialog(activity, "Remove group succeed")
            } else {
                showDeleteGroupLocallyDialog()
            }
        }

    }

}