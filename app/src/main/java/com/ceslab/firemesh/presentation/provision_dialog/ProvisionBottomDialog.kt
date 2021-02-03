package com.ceslab.firemesh.presentation.provision_dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.factory.ViewModelFactory
import com.ceslab.firemesh.meshmodule.model.ConnectableDeviceDescription
import com.ceslab.firemesh.meshmodule.model.MeshStatus
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.network.NetworkFragment
import com.ceslab.firemesh.presentation.node.NodeFragment
import com.ceslab.firemesh.presentation.scan.ScanViewModel
import com.ceslab.firemesh.util.AndroidDialogUtil
import com.ceslab.firemesh.util.AppUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siliconlab.bluetoothmesh.adk.ErrorType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_provision_bottom_sheet.view.*
import timber.log.Timber
import javax.inject.Inject

class ProvisionBottomDialog : BottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    lateinit var mAgrs: Bundle
    private var networkIndex = 0

    private lateinit var provisionDialogViewModel: ProvisionDialogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme)
        mAgrs = arguments!!
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

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        provisionDialogViewModel = ViewModelProvider(this, viewModelFactory).get(ProvisionDialogViewModel::class.java)
        provisionDialogViewModel.isProvisioningSucceed.observe(this,isProvisioningSucceedObserver)
        provisionDialogViewModel.errorMessage.observe(this,onProvisioningErrorObserver)
    }

    private val onProvisionButtonClicked = View.OnClickListener {
        activity?.runOnUiThread {
            val nodeName = view!!.edt_node_name.text.toString()
            view!!.spinner_network.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View,
                        position: Int,
                        id: Long
                    ) {
                        networkIndex = position
                    }
                }
            if (AppUtil.isDeviceNameValid(nodeName)) {
                 val deviceDescription = mAgrs.getSerializable("ConnectableDeviceDescription") as ConnectableDeviceDescription
                dialog?.hide()
                AndroidDialogUtil.getInstance().showLoadingDialog(activity,"Provisioning...")
                provisionDialogViewModel.provisionDevice(deviceDescription,networkIndex)
            }
        }
    }

    private val isProvisioningSucceedObserver = Observer<Boolean> {
        it.let {
            if(it){
                AndroidDialogUtil.getInstance().hideDialog()
                val mainActivity = activity as MainActivity
                mainActivity.replaceFragment(NodeFragment(), NodeFragment.TAG,R.id.container_main)
            }
        }
    }

    private val onProvisioningErrorObserver = Observer<ErrorType> {
        it.let {
            AndroidDialogUtil.getInstance().showFailureDialog(activity,"Provision Failed: errorCode:${it.errorCode}")

        }
    }
}