package com.ceslab.firemesh.presentation.node.node_config

import android.app.AlertDialog
import android.graphics.Color
import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.ConfigurationTask
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.model.NodeConfig
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality.Companion.getFunctionalitiesNamed
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.node.node_config.dialog.ModelConfigCallback
import com.ceslab.firemesh.presentation.node.node_config.dialog.ModelConfigDialog
import com.ceslab.firemesh.presentation.node_list.NodeListRecyclerViewAdapter
import com.ceslab.firemesh.presentation.node_list.dialog.DeleteNodeDialog
import com.ceslab.firemesh.presentation.subnet.SubnetFragment
import com.ceslab.firemesh.util.AppUtil
import com.siliconlab.bluetoothmesh.adk.ErrorType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_node_config.*
import kotlinx.android.synthetic.main.fragment_node_list.*
import timber.log.Timber

class NodeConfigFragment : BaseFragment() {

    private lateinit var nodeConfigViewModel: NodeConfigViewModel
    private lateinit var functionalityRecyclerViewAdapter: FunctionalityRecyclerViewAdapter

    override fun getResLayoutId(): Int {
        return R.layout.fragment_node_config
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupRecyclerView()
        setupViewModel()
        setFeaturesOnClickListeners()

    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        nodeConfigViewModel.removeConfigListeners()
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        nodeConfigViewModel =
            ViewModelProvider(this, viewModelFactory).get(NodeConfigViewModel::class.java)
        nodeConfigViewModel.apply {
            setConfigListeners()
            getNodeConfig().observe(this@NodeConfigFragment, nodeConfigObserver)
            getProxyStatus().observe(this@NodeConfigFragment, proxyStatusObserver)
            getRelayStatus().observe(this@NodeConfigFragment, relayStatusObserver)
            getFriendStatus().observe(this@NodeConfigFragment, friendStatusObserver)
            getRetransmissionStatus().observe(this@NodeConfigFragment, retransmissionStatusObserver)
            getCurrentConfigTask().observe(this@NodeConfigFragment, configurationStatusObserver)
            getConfigurationError().observe(this@NodeConfigFragment, configurationErrorObserver)
            getProxyAttention().observe(this@NodeConfigFragment, proxyAttentionObserver)
        }

    }

    private fun setupNodeFeatureConfig(nodeConfig: NodeConfig) {
        Timber.d("setupNodeFeatureConfig")
        activity?.runOnUiThread {
            nodeConfig.apply {
                isSupportProxy?.let { isSupportProxy ->
                    Timber.d("isSupportProxy = $isSupportProxy")
                    if (isSupportProxy) {
                        ll_proxy.visibility = View.VISIBLE
                    } else {
                        ll_proxy.visibility = View.GONE
                    }
                }
                isSupportFriend?.let { isSupportFriend ->
                    Timber.d("isSupportFriend = $isSupportFriend")
                    if (isSupportFriend) {
                        ll_friend.visibility = View.VISIBLE
                    } else {
                        ll_friend.visibility = View.GONE
                    }
                }
                isSupportRelay?.let { isSupportRelay ->
                    Timber.d("isSupportRelay = $isSupportRelay")
                    if (isSupportRelay) {
                        ll_relay.visibility = View.VISIBLE
                    } else {
                        ll_relay.visibility = View.GONE
                    }
                }

                isSupportLowPower?.let {
                    Timber.d("isSupportLowPower = $it")
                    if (it) {
                        tv_low_power_support.text = "Is Supported"
                        tv_low_power_support.setTextColor(Color.parseColor("#4CAF50"))
                    } else {
                        tv_low_power_support.text = "No Supported"
                        tv_low_power_support.setTextColor(Color.parseColor("#F44336"))

                    }
                }
            }

            //SETUP CHANGE SWITCH STATUS
            sw_proxy.setOnCheckedChangeListener { _, isChecked ->
                nodeConfigViewModel.changeProxy(isChecked)
            }
            sw_relay.setOnCheckedChangeListener { _, isChecked ->
                nodeConfigViewModel.changeRelay(isChecked)
            }
            sw_friend.setOnCheckedChangeListener { _, isChecked ->
                nodeConfigViewModel.changeFriend(isChecked)
            }
            sw_retransmission.setOnCheckedChangeListener { _, isChecked ->
                nodeConfigViewModel.changeRetransmission(isChecked)
            }
        }

    }

    private fun setFeaturesOnClickListeners() {
        btn_get_proxy.setOnClickListener {
            nodeConfigViewModel.updateProxy()
        }
        btn_get_relay.setOnClickListener {
            nodeConfigViewModel.updateRelay()
        }
        btn_get_friend.setOnClickListener {
            nodeConfigViewModel.updateFriend()
        }
        btn_get_retransmission.setOnClickListener {
            nodeConfigViewModel.updateRetransmission()
        }
    }

    private fun setupGroupSpinner(meshNode: MeshNode) {
        Timber.d("setupGroupSpinner")
        activity?.runOnUiThread {
            meshNode.apply {
                val groupListInSubnet = node.subnets.first().groups.sortedBy { it.name }
                val groupNameList = mutableListOf("").apply {
                    addAll(groupListInSubnet.map { it.name })
                }
                val groupAdapter = ArrayAdapter<String>(
                    context!!,
                    android.R.layout.simple_spinner_item,
                    groupNameList
                )
                groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                spinner_group.onItemSelectedListener = null
                spinner_group.adapter = groupAdapter
                if (node.groups.isNotEmpty()) {
                    Timber.d("Node group is not empty")
                    val groupInfo = node.groups.first()
                    groupListInSubnet.find { it == groupInfo }
                        ?.let {
                            spinner_group.setSelection(groupNameList.indexOf(it.name), false)
                        }
                    ll_functionality.visibility = View.VISIBLE

                } else {
                    Timber.d("Node group is empty")
                    spinner_group.setSelection(Adapter.NO_SELECTION, false)
                    ll_functionality.visibility = View.GONE
                }
                spinner_group.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (position == 0) {
                            ll_functionality.visibility = View.GONE
                            nodeConfigViewModel.changeGroup(null)
                        } else {
                            nodeConfigViewModel.changeGroup(groupListInSubnet[position - 1])

                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }


    private fun setupRecyclerView() {
        Timber.d("setupRecyclerView")
        val linearLayoutManager = LinearLayoutManager(view!!.context)
        functionalityRecyclerViewAdapter = FunctionalityRecyclerViewAdapter(view!!.context)
        functionalityRecyclerViewAdapter.itemClickListener = object :
            BaseRecyclerViewAdapter.ItemClickListener<NodeFunctionality.FunctionalityNamed> {
            override fun onClick(position: Int, item: NodeFunctionality.FunctionalityNamed) {
                Timber.d("onClick: $item")
                ViewCompat.postOnAnimationDelayed(view!!, // Delay to show ripple effect
                    Runnable {
                        val modelConfigDialog = ModelConfigDialog(item)
                        modelConfigDialog.setModelConfigCallback(object : ModelConfigCallback {
                            override fun onCancel() {
                                Timber.d("onCancel")
                            }
                        })
                        modelConfigDialog.show(fragmentManager!!, "ModelConfigDialog")
                    }
                    ,50)

            }

            override fun onLongClick(position: Int, item: NodeFunctionality.FunctionalityNamed) {}
        }
        rv_functionality.apply {
            layoutManager = linearLayoutManager
            setHasFixedSize(true)
            adapter = functionalityRecyclerViewAdapter
        }
    }

    private fun setupFunctionalitySpinner(meshNode: MeshNode) {
//        Timber.d("setupFunctionalitySpinner: $")
//        activity?.runOnUiThread {
//            meshNode.apply {
//                val functionalitiesNamed =
//                    NodeFunctionality.getFunctionalitiesNamed(node).toMutableList()
//                functionalitiesNamed.sortBy { it.functionalityName }
//
//                val functionalitiesName = functionalitiesNamed.map { it.functionalityName }
//                val functionalityAdapter = ArrayAdapter<String>(
//                    context!!,
//                    android.R.layout.simple_spinner_item,
//                    functionalitiesName
//                )
//                functionalityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//                spinner_functionality.apply {
//                    onItemSelectedListener = null
//                    adapter = functionalityAdapter
//                    if (functionality != NodeFunctionality.VENDOR_FUNCTIONALITY.Unknown) {
//                        functionalitiesNamed.indexOfFirst { it.functionality == functionality }
//                            .takeUnless { it == -1 }
//                            ?.let { index ->
//                                setSelection(index, false)
//                            }
//
//                    } else {
//                        setSelection(Adapter.NO_SELECTION, false)
//                    }
//
//                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                        override fun onItemSelected(
//                            parent: AdapterView<*>?,
//                            view: View?,
//                            position: Int,
//                            id: Long
//                        ) {
//                            Timber.d("onItemSelected: $position")
//                            if (position != 0) {
////                                val modelConfigDialog =
////                                    ModelConfigDialog(functionalitiesNamed[position].functionality)
////                                modelConfigDialog.setModelConfigCallback(object :
////                                    ModelConfigCallback {
////                                    override fun onCancel() {
////                                        Timber.d("onCancel")
////                                        setSelection(0, false)
////                                    }
////                                })
////                                modelConfigDialog.show(fragmentManager!!, "ModelConfigDialog")
//                            }
//
//                        }
//
//                        override fun onNothingSelected(parent: AdapterView<*>?) {}
//                    }
//                }
//            }
//        }
    }

    private val configurationStatusObserver = Observer<ConfigurationTask> {
        Timber.d("configurationStatusObserver: $it")
        activity?.runOnUiThread {
            when (it) {
                ConfigurationTask.CONFIG_BIND_NODE_TO_GROUP -> showProgressDialog("Binding node to group")
                ConfigurationTask.CONFIG_UNBIND_NODE_FROM_GROUP -> showProgressDialog("Unbinding node from group")
                ConfigurationTask.CONFIG_BIND_MODEL_TO_GROUP -> showProgressDialog("Binding model to group")
                ConfigurationTask.CONFIG_UNBIND_MODEL_FROM_GROUP -> showProgressDialog("Unbinding model from group")
                ConfigurationTask.CONFIG_PUBLICATION_SETTING -> showProgressDialog("Setting publications")
                ConfigurationTask.CONFIG_PUBLICATION_CLEARING -> showProgressDialog("Clearing publications")
                ConfigurationTask.CONFIG_SUBSCRIPTION_ADDING -> showProgressDialog("Adding subscriptions")
                ConfigurationTask.CONFIG_SUBSCRIPTION_REMOVING -> showProgressDialog("Removing subscriptions")
                ConfigurationTask.CONFIG_PROXY_CHECKING -> showProgressDialog("Checking proxy status")
                ConfigurationTask.CONFIG_PROXY_ENABLING -> showProgressDialog("Enabling proxy")
                ConfigurationTask.CONFIG_PROXY_DISABLING -> showProgressDialog("Disabling proxy")
                ConfigurationTask.CONFIG_RELAY_CHECKING -> showProgressDialog("Checking relay status")
                ConfigurationTask.CONFIG_RELAY_ENABLING -> showProgressDialog("Enabling relay")
                ConfigurationTask.CONFIG_RELAY_DISABLING -> showProgressDialog("Disabling relay")
                ConfigurationTask.CONFIG_FRIEND_CHECKING -> showProgressDialog("Checking friend status")
                ConfigurationTask.CONFIG_FRIEND_ENABLING -> showProgressDialog("Enabling friend")
                ConfigurationTask.CONFIG_FRIEND_DISABLING -> showProgressDialog("Disabling friend")
                ConfigurationTask.CONFIG_RETRANSMISSION_CHECKING -> showProgressDialog("Checking retransmission status")
                ConfigurationTask.CONFIG_RETRANSMISSION_ENABLING -> showProgressDialog("Enabling retransmission")
                ConfigurationTask.CONFIG_RETRANSMISSION_DISABLING -> showProgressDialog("Disabling retransmission")
                ConfigurationTask.CONFIG_CONTROL_NODE_SUCCEED ->  {
                    ll_functionality.visibility = View.VISIBLE
                    hideDialog()
                }
                ConfigurationTask.CONFIG_CONTROL_MODEL_SUCCEED -> {

                    hideDialog()
                }
                ConfigurationTask.CONFIG_CONTROL_PUBLICATION_SUCCEED -> hideDialog()
                ConfigurationTask.CONFIG_CONTROL_SUBSCRIPTION_SUCCEED -> hideDialog()
            }
        }
    }

    private val configurationErrorObserver = Observer<ErrorType> {
        activity?.runOnUiThread {
            showFailedDialog(AppUtil.convertErrorMessage(activity!!, it))
        }
    }

    private val nodeConfigObserver = Observer<NodeConfig> {
        Timber.d("nodeConfigObserver=${it.meshNode.functionalityList}")
        activity?.runOnUiThread {
            setupNodeFeatureConfig(it)
            setupGroupSpinner(it.meshNode)
            val functionalityNamedList = getFunctionalitiesNamed(it.meshNode.node)
            for(functionalityNamed in functionalityNamedList ){
                if(it.meshNode.functionalityList.contains(functionalityNamed.functionality)){
                    functionalityNamed.functionality.isBinded = true
                }
            }

            for(test in functionalityNamedList){
                Timber.d("TEST= ${test.functionalityName} -- isBinded=${test.functionality.isBinded}")
            }

            functionalityRecyclerViewAdapter.setDataList(functionalityNamedList.toMutableList())
           // setupFunctionalitySpinner(it.meshNode)
        }
    }


    private val proxyStatusObserver = Observer<Boolean> { isEnabled ->
        activity?.runOnUiThread {
            hideDialog()
            sw_proxy.isChecked = isEnabled
        }
    }


    private val relayStatusObserver = Observer<Boolean> { isEnabled ->
        activity?.runOnUiThread {
            hideDialog()
            sw_relay.isChecked = isEnabled
        }
    }


    private val friendStatusObserver = Observer<Boolean> { isEnabled ->
        activity?.runOnUiThread {
            hideDialog()
            sw_friend.isChecked = isEnabled
        }
    }


    private val retransmissionStatusObserver = Observer<Boolean> { isEnabled ->
        activity?.runOnUiThread {
            hideDialog()
            sw_retransmission.isChecked = isEnabled
        }
    }

    private val proxyAttentionObserver = Observer<Boolean> {
        Timber.d("proxyAttentionObserver = $it")
        activity?.runOnUiThread {
            if (it == true) {
                val builder =
                    AlertDialog.Builder(activity, R.style.AlertDialogTheme)
                builder.apply {
                    setTitle("Attention")
                    setMessage("Disabling this proxy will cause you to lose access to the network. Continue anyways?")
                    setPositiveButton("OK") { dialog, _ ->
                        nodeConfigViewModel.processChangeProxy(false)
                        dialog.dismiss()
                    }

                    setNegativeButton("Cancel") { dialog, _ ->
                        sw_proxy.isChecked = true
                        hideDialog()
                        dialog.dismiss()
                    }
                }
                builder.create().show()
            }
        }
    }

}