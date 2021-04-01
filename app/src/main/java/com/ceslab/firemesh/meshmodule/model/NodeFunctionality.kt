package com.ceslab.firemesh.meshmodule.model

import com.siliconlab.bluetoothmesh.adk.data_model.model.VendorModel
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node
import timber.log.Timber

/**
 * Created by Viet Hua on 11/23/2020.
 */

class NodeFunctionality {

    enum class VENDOR_FUNCTIONALITY(var isSupportPublication:Boolean,var isSupportSubscription:Boolean,var isBinded: Boolean,vararg val model: VendorModelIdentifier) {
        Unknown(false,false,false,VendorModelIdentifier.Unknown),
        NodeStatusClient(true,true,false,VendorModelIdentifier.NodeStatusClient),
        NodeStatusServer(true,true,false,VendorModelIdentifier.NodeStatusServer),
        GatewayStatusClient(true,true,false,VendorModelIdentifier.GatewayStatusClient),
        GatewayStatusServer(true,false,false,VendorModelIdentifier.GatewayStatusServer);


        fun getAllModels(): Set<VendorModelIdentifier> {
            val models = mutableSetOf<VendorModelIdentifier>()
            models.addAll(model)
            return models
        }

        companion object {
            fun fromId(
                companyIdentifierId: Int,
                assignedModelIdentifier: Int
            ): VENDOR_FUNCTIONALITY? {
                return VendorModelIdentifier.values()
                    .find { it.companyIdentifier == companyIdentifierId && it.assignedModelIdentifier == assignedModelIdentifier }
                    ?.let { modelIdentifier ->
                        values().find { it.model.contains(modelIdentifier) }
                    }
            }
        }
    }

    data class FunctionalityNamed(
        val functionality: VENDOR_FUNCTIONALITY,
        val functionalityName: String
    )

    companion object {

        fun getFunctionalitiesNamed(node: Node): Set<FunctionalityNamed> {
            return mutableSetOf<FunctionalityNamed>().apply {
                addAll(node.elements?.flatMap { it.vendorModels }
                    ?.mapNotNull { vendorModel ->
                        VENDOR_FUNCTIONALITY.fromId(
                            vendorModel.vendorCompanyIdentifier(),
                            vendorModel.vendorAssignedModelIdentifier()
                        )?.let {
                            var vendorModelName = "Unknown Model"
                            if(it.name == VENDOR_FUNCTIONALITY.NodeStatusClient.name){
                                vendorModelName = "Node Status Client"
                            } else if(it.name == VENDOR_FUNCTIONALITY.NodeStatusServer.name) {
                                vendorModelName = "Node Status Server"
                            }  else if(it.name == VENDOR_FUNCTIONALITY.GatewayStatusClient.name) {
                                vendorModelName = "Gateway Status Client"
                            }  else if(it.name == VENDOR_FUNCTIONALITY.GatewayStatusServer.name) {
                                vendorModelName = "Gateway Status Server"
                            }
                            FunctionalityNamed(it, vendorModelName)
                        }
                    } ?: emptySet())
            }
        }

        fun getVendorModels(node: Node, functionality: VENDOR_FUNCTIONALITY): Set<VendorModel> {
            val supportedModelIds = functionality.getAllModels()
            Timber.d("getVendorModels=${functionality.getAllModels()}")
            return node.elements?.flatMap { it.vendorModels }
                ?.filter { vendorModel ->
                    supportedModelIds.any {
                        it.companyIdentifier == vendorModel.vendorCompanyIdentifier() && it.assignedModelIdentifier == vendorModel.vendorAssignedModelIdentifier()
                    }
                }
                ?.toSet() ?: emptySet()
        }
    }
}