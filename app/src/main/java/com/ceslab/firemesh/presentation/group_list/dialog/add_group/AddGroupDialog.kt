package com.ceslab.firemesh.presentation.group_list.dialog.add_group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.factory.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_add_group_bottom_sheet.view.*

import timber.log.Timber
import javax.inject.Inject

class AddGroupDialog : BottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private lateinit var addGroupClickListener: AddGroupClickListener
    private lateinit var addGroupViewModel: AddGroupViewModel

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
        val view = inflater.inflate(R.layout.dialog_add_group_bottom_sheet, container, false)
        view.btn_add_group.setOnClickListener(onAddGroupButtonClicked)
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
    fun setAddGroupClickListener(addGroupClickListener: AddGroupClickListener){
        this.addGroupClickListener = addGroupClickListener
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        addGroupViewModel = ViewModelProvider(this, viewModelFactory).get(AddGroupViewModel::class.java)

    }

    private val onAddGroupButtonClicked = View.OnClickListener {
            val groupName = view!!.edt_group_name.text.toString()
            addGroupViewModel.addGroup(groupName)
            addGroupClickListener.onClicked()
            dialog?.dismiss()
    }
}