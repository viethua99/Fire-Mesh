package com.ceslab.firemesh.presentation.provision_list.dialog

import android.content.DialogInterface
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
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.node.NodeFragment
import com.ceslab.firemesh.util.AndroidDialogUtil
import com.ceslab.firemesh.util.AppUtil
import com.ceslab.firemesh.util.ConverterUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siliconlab.bluetoothmesh.adk.ErrorType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_provision_bottom_sheet.*
import kotlinx.android.synthetic.main.dialog_provision_bottom_sheet.view.*
import timber.log.Timber
import javax.inject.Inject

class ProvisionBottomDialog : BottomSheetDialogFragment() {

    companion object {
        const val IS_FIRST_CONFIG_KEY = "IS_FIRST_CONFIG_KEY"
        const val DEVICE_DESCRIPTION_KEY = "DEVICE_DESCRIPTION_KEY"
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var deviceDescription: ConnectableDeviceDescription
    private var networkIndex = 0

    private lateinit var provisionDialogViewModel: ProvisionDialogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme)
        arguments?.let {
            if (it.containsKey(DEVICE_DESCRIPTION_KEY)) {
                deviceDescription =
                    it.getSerializable(DEVICE_DESCRIPTION_KEY) as ConnectableDeviceDescription
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("onCreateView")
        val view = inflater.inflate(R.layout.dialog_provision_bottom_sheet, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        setupViewModel()
        setupNetworkSpinner(view)
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
        provisionDialogViewModel = ViewModelProvider(this, viewModelFactory).get(
            ProvisionDialogViewModel::class.java
        )
        provisionDialogViewModel.isProvisioningSucceed.observe(this, isProvisioningSucceedObserver)
        provisionDialogViewModel.errorMessage.observe(this, onProvisioningErrorObserver)
    }

    private fun setupNetworkSpinner(view: View) {
        Timber.d("setupNetworkSpinner")
        activity?.runOnUiThread {
            if(deviceDescription.deviceName != null){
                view.edt_node_name.setText(deviceDescription.deviceName)
            } else {
                view.edt_node_name.setText(deviceDescription.deviceAddress?.takeLast(5))
            }

            val adapter = ArrayAdapter(
                context!!,
                android.R.layout.simple_spinner_item,
                provisionDialogViewModel.getNetworkNameList()
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            view.spinner_network.adapter = adapter
            view.btn_provision.setOnClickListener(onProvisionButtonClicked)
        }

    }

    private val onProvisionButtonClicked = View.OnClickListener {
        activity?.runOnUiThread {
            val nodeName = view!!.edt_node_name.text.toString()
            view!!.spinner_network.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                        networkIndex = position
                    }
                }
            if (AppUtil.isNameValid(nodeName)) {
                AndroidDialogUtil.getInstance().showLoadingDialog(activity, "Provisioning...")
                provisionDialogViewModel.provisionDevice(deviceDescription, networkIndex,nodeName)
            }
        }
    }

    private val isProvisioningSucceedObserver = Observer<String> {nodeName ->
        Timber.d("isProvisioningSucceedObserver: $nodeName")
        nodeName.let {
                dialog?.dismiss()
                AndroidDialogUtil.getInstance().hideDialog()
                val mainActivity = activity as MainActivity
                val args = Bundle()
                args.putBoolean(IS_FIRST_CONFIG_KEY, true)
                val nodeFragment = NodeFragment(it)
                nodeFragment.arguments = args
                mainActivity.replaceFragment(nodeFragment, NodeFragment.TAG, R.id.container_main)
        }
    }

    private val onProvisioningErrorObserver = Observer<ErrorType> {
        Timber.d("onProvisioningErrorObserver: $it")
        it.let {
            AndroidDialogUtil.getInstance()
                .showFailureDialog(activity, "Provision Failed: errorCode:${it.type}")

        }
    }
}