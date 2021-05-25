package com.ceslab.firemesh.service

import com.ceslab.firemesh.meshmodule.model.MeshNode

/**
 * Created by Viet Hua on 04/20/2021.
 */

data class FireNodeStatus(val batteryPercent: Int = 0 ,val unicastAddress:String = "",val gatewayType: MeshNode.GatewayType = MeshNode.GatewayType.NOT_GATEWAY )


