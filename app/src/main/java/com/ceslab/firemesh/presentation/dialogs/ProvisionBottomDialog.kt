package com.ceslab.firemesh.presentation.dialogs

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.node.NodeFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.dialog_provision_bottom_sheet.view.*
import kotlinx.android.synthetic.main.fragment_scan.*
import timber.log.Timber
import util.AndroidDialogUtil

class ProvisionBottomDialog : BottomSheetDialogFragment() {
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
        val view = inflater.inflate(R.layout.dialog_provision_bottom_sheet, container, false)
        val list = listOf("Test1", "Test2", "Test3")
        val adapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item, list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        view.spinner_network.adapter = adapter
        view.btn_provision.setOnClickListener(onProvisionButtonClicked)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        dialog?.setOnShowListener {
            val dialog = it as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                sheet.parent.parent.requestLayout()
            }
        }
    }

    private val onProvisionButtonClicked = View.OnClickListener {
        dialog?.hide()
        val mainActivity = activity as MainActivity
        AndroidDialogUtil.getInstance().showLoadingDialog(mainActivity,"Provisioning...")
        Handler().postDelayed(Runnable {
            AndroidDialogUtil.getInstance().hideDialog()
            mainActivity.replaceFragment(NodeFragment(), NodeFragment.TAG, R.id.container_main)
        }, 5000)

    }
}