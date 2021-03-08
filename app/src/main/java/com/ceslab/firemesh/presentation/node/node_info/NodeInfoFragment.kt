package com.ceslab.firemesh.presentation.node.node_info

import android.view.View
import android.widget.TableRow
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.model.ModelTableDescription
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.ceslab.firemesh.presentation.base.BaseFragment
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.util.ConverterUtil
import com.siliconlab.bluetoothmesh.adk.data_model.model.Model
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_node_info.*
import kotlinx.android.synthetic.main.item_model_table_row.view.*
import kotlinx.android.synthetic.main.item_subnet.*
import timber.log.Timber

class NodeInfoFragment : BaseFragment() {

    private lateinit var nodeInfoViewModel: NodeInfoViewModel

    override fun getResLayoutId(): Int {
        return R.layout.fragment_node_info
    }

    override fun onMyViewCreated(view: View) {
        Timber.d("onMyViewCreated")
        setupViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        nodeInfoViewModel.removeMeshConfigurationLoadedListener()
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")
        AndroidSupportInjection.inject(this)
        nodeInfoViewModel =
            ViewModelProvider(this, viewModelFactory).get(NodeInfoViewModel::class.java)
        nodeInfoViewModel.getMeshNodeToConfigure().observe(this, meshNodeToConfigureObserver)
    }

    private fun showMeshNodeInfo(meshNode: MeshNode) {
        Timber.d("showMeshNodeInfo: ${meshNode.node.name}")
        activity?.runOnUiThread {
            tv_node_name.text = meshNode.node.name
            tv_node_unicast_address.text = meshNode.node.primaryElementAddress!!.toString()
            tv_node_uuid.text = ConverterUtil.getHexValue(meshNode.node.uuid)

            val network = meshNode.node.subnets.iterator().next()
            tv_node_subnet_name.text = network.name

            tv_node_network_key.text = ConverterUtil.getHexValue(network.netKey.key)
            if (meshNode.node.groups.size > 0) {
                tv_node_app_key.text = ConverterUtil.getHexValue(meshNode.node.groups.iterator().next().appKey.key)
            }

            tv_node_dev_key.text = ConverterUtil.getHexValue(meshNode.node.devKey.key)
            fillModelsTable(meshNode)
        }
    }

    private fun fillModelsTable(meshNode: MeshNode) {
        Timber.d("fillModelsTable")
        var tableIndex = 0
        meshNode.node.elements?.forEachIndexed { elementIndex, element ->
            val models = mutableListOf<Model>()
            models.apply {
                addAll(element.sigModels)
                addAll(element.vendorModels)

                forEach { model ->
                    Timber.d("model:${model.name}")
                    val modelName = model.name
                    val modelType: String
                    val modelId: String

                    if (model.isSIGModel) {
                        modelType = "SIG"

                        val sigInfo = ConverterUtil.inv_atou16(model.id)
                        modelId = "0x" + ConverterUtil.getHexValueNoSpace(sigInfo)
                    } else {
                        val vendorInfo = ConverterUtil.inv_atou32(model.id)
                        val vendorType = byteArrayOf(vendorInfo[2], vendorInfo[3])
                        val vendorId = byteArrayOf(vendorInfo[0], vendorInfo[1])

                        modelType = "0x" + ConverterUtil.getHexValueNoSpace(vendorType)
                        modelId = "0x" + ConverterUtil.getHexValueNoSpace(vendorId)
                    }

                    val modelInfo = ModelTableDescription(elementIndex, modelType, modelId, modelName)
                    table_models.addView(createRowElement(modelInfo))
                    tableIndex++
                }
            }

        }
    }

    private fun createRowElement(modelTableDescription: ModelTableDescription): TableRow {
        val row = layoutInflater.inflate(R.layout.item_model_table_row, null) as TableRow
        row.apply {
            cell_element.text = modelTableDescription.elementIndex.toString()
            cell_vendor.text = modelTableDescription.modelType
            cell_id.text = modelTableDescription.modelId
            cell_description.text = modelTableDescription.modelName
            cell_description.isSelected = true
        }
        return row
    }


    private val meshNodeToConfigureObserver = Observer<MeshNode> {
        showMeshNodeInfo(it)
    }
}