package com.ceslab.firemesh.presentation.node.node_config

import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.ConfigurationStatus
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.ceslab.firemesh.presentation.base.BaseFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_node_config.*
import timber.log.Timber

class NodeConfigFragment : BaseFragment() {

    private lateinit var nodeConfigViewModel: NodeConfigViewModel

    override fun getResLayoutId(): Int {
        return R.layout.fragment_node_config
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupViewModel()
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
        nodeConfigViewModel.setConfigListeners()
        nodeConfigViewModel.getMeshNodeToConfigure().observe(this, meshNodeToConfigureObserver)
        nodeConfigViewModel.getRelayStatus().observe(this, relayStatusObserver)
        nodeConfigViewModel.getFriendStatus().observe(this, friendStatusObserver)
        nodeConfigViewModel.getProxyStatus().observe(this, proxyStatusObserver)
        nodeConfigViewModel.getConfigurationStatus().observe(this, configurationStatusObserver)


    }

    private fun setupNodeFeatureConfig(meshNode: MeshNode) {
        Timber.d("setupNodeFeatureConfig")
        activity?.runOnUiThread {
            meshNode.apply {
                val deviceCompositionData = node.deviceCompositionData
                deviceCompositionData?.apply {
                    val isSupportProxy = supportsProxy()
                    val isSupportFriend = supportsFriend()
                    val isSupportRelay = supportsRelay()
                    val isSupportLowPower = supportsLowPower()
                    Timber.d(
                        "supportProxy: ${isSupportProxy} " +
                                "--- supportFriend:${isSupportFriend}" +
                                "--- supportRelay: ${isSupportRelay}" +
                                "--- supportLpn: ${isSupportLowPower}"
                    )

                    if (isSupportFriend) sw_friend.visibility =
                        View.VISIBLE else sw_friend.visibility = View.GONE
                    if (isSupportProxy) sw_proxy.visibility =
                        View.VISIBLE else sw_proxy.visibility = View.GONE
                    if (isSupportRelay) sw_relay.visibility =
                        View.VISIBLE else sw_relay.visibility = View.GONE
                    if (isSupportLowPower) tv_low_power_support.text =
                        "Is Supported" else "No Supported"

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
            }
        }
    }

    private fun setupGroupSpinner(meshNode: MeshNode) {
        Timber.d("setupGroupSpinner")
        activity?.runOnUiThread {
            meshNode.apply {
                val groupListInSubnet = node.subnets.first().groups.sortedBy { it.name }
                val groupNameList = ArrayList<String>()
                groupNameList.add("")
                groupListInSubnet.forEach { groupInfo ->
                    groupNameList.add(groupInfo.name)
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
                    val groupInfo = groupListInSubnet.find { group ->
                        group == node.groups.iterator().next()
                    }
                    groupInfo?.apply {
                        spinner_group.setSelection(groupNameList.indexOf(name), false)
                    }
                } else if (groupListInSubnet.isNotEmpty()) {
                    spinner_group.setSelection(Adapter.NO_SELECTION, false)
                }
                spinner_group.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (position == 0) {
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

    private fun setupFunctionalitySpinner(meshNode: MeshNode) {
        Timber.d("setupFunctionalitySpinner")
        activity?.runOnUiThread {
            meshNode.apply {
                val functionalitiesNamed =
                    NodeFunctionality.getFunctionalitiesNamed(node).toMutableList()
                functionalitiesNamed.sortBy { it.functionalityName }

                val functionalitiesName = functionalitiesNamed.map { it.functionalityName }
                val functionalityAdapter = ArrayAdapter<String>(
                    context!!,
                    android.R.layout.simple_spinner_item,
                    functionalitiesName
                )
                functionalityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                spinner_functionality.onItemSelectedListener = null
                spinner_functionality.adapter = functionalityAdapter
                if (functionality != NodeFunctionality.VENDOR_FUNCTIONALITY.Unknown) {
                    functionalitiesNamed.find { it.functionality == functionality }
                        ?.let { functionalityNamed ->
                            spinner_functionality.setSelection(
                                functionalitiesNamed.indexOf(
                                    functionalityNamed
                                ), false
                            )
                        }

                } else {
                    spinner_functionality.setSelection(Adapter.NO_SELECTION, false)
                }
                spinner_functionality.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            nodeConfigViewModel.changeFunctionality(functionalitiesNamed[position].functionality)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
            }
        }
    }

    private val meshNodeToConfigureObserver = Observer<MeshNode> {
        setupNodeFeatureConfig(it)
        setupGroupSpinner(it)
        setupFunctionalitySpinner(it)
    }

    private val friendStatusObserver = Observer<Boolean> {
        activity?.runOnUiThread {
            sw_friend.isChecked = it
        }
    }
    private val relayStatusObserver = Observer<Boolean> {
        activity?.runOnUiThread {
            sw_relay.isChecked = it
        }
    }
    private val proxyStatusObserver = Observer<Boolean> {
        activity?.runOnUiThread {
            sw_proxy.isChecked = it
        }
    }

    private val configurationStatusObserver = Observer<ConfigurationStatus> {
        activity?.runOnUiThread {
            when (it) {
                ConfigurationStatus.BIND_NODE_TO_GROUP -> showProgressDialog("Bind node to group")
                ConfigurationStatus.UNBIND_NODE_FROM_GROUP -> showProgressDialog("Unbind node from group")
                ConfigurationStatus.BIND_MODEL_TO_GROUP -> showProgressDialog("Bind model to group")
                ConfigurationStatus.UNBIND_MODEL_FROM_GROUP -> showProgressDialog("Unbind model from group")
                ConfigurationStatus.SET_PUBLICATION_SETTING -> showProgressDialog("Set publication settings")
                ConfigurationStatus.CLEAR_PUBLICATION_SETTING -> showProgressDialog("Clear publication settings")
                ConfigurationStatus.ADD_SUBSCRIPTION_SETTING -> showProgressDialog("Add subscription settings")
                ConfigurationStatus.REMOVE_SUBSCRIPTION_SETTING -> showProgressDialog("Remove subscription settings")
                ConfigurationStatus.NODE_CONFIG_FINISHED -> hideDialog()
            }
        }
    }

}