package com.ceslab.firemesh.presentation.subnet_list.dialog.add_subnet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.factory.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_add_subnet_bottom_sheet.view.*
import timber.log.Timber
import javax.inject.Inject
import android.widget.Toast


class AddSubnetDialog : BottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private lateinit var addSubnetClickListener: AddSubnetClickListener
    private lateinit var addSubnetViewModel: AddSubnetViewModel

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
        val view = inflater.inflate(R.layout.dialog_add_subnet_bottom_sheet, container, false)
        view.btn_add_subnet.setOnClickListener(onAddSubnetButtonClicked)
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
    fun setAddSubnetClickListener(addSubnetClickListener: AddSubnetClickListener){
        this.addSubnetClickListener = addSubnetClickListener
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        addSubnetViewModel = ViewModelProvider(this, viewModelFactory).get(AddSubnetViewModel::class.java)
        addSubnetViewModel.errorMessage.observe(this, Observer { errorMessage ->
            Toast.makeText(context,errorMessage,Toast.LENGTH_SHORT).show()
        })

    }

    private val onAddSubnetButtonClicked = View.OnClickListener {
        activity?.runOnUiThread {
            val networkName = view!!.edt_subnet_name.text.toString()
            addSubnetViewModel.addSubnet(networkName)
            addSubnetClickListener.onClicked()
            dialog?.dismiss()

        }
    }

}