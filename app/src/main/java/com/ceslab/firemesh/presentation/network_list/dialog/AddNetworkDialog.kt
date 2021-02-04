package com.ceslab.firemesh.presentation.network_list.dialog

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
import kotlinx.android.synthetic.main.dialog_add_network_bottom_sheet.view.*
import timber.log.Timber
import javax.inject.Inject

class AddNetworkDialog : BottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private lateinit var addNetworkClickListener: AddNetworkClickListener
    private lateinit var addNetworkViewModel: AddNetworkViewModel

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
        val view = inflater.inflate(R.layout.dialog_add_network_bottom_sheet, container, false)
        view.btn_add_network.setOnClickListener(onAddNetworkButtonClicked)
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
    fun setAddNetworkClickListener(addNetworkClickListener: AddNetworkClickListener){
        this.addNetworkClickListener = addNetworkClickListener
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        addNetworkViewModel = ViewModelProvider(this, viewModelFactory).get(AddNetworkViewModel::class.java)

    }

    private val onAddNetworkButtonClicked = View.OnClickListener {
        activity?.runOnUiThread {
            val networkName = view!!.edt_network_name.text.toString()
            addNetworkViewModel.addNetwork(networkName)
            addNetworkClickListener.onClicked()
            dialog?.dismiss()

        }
    }

}