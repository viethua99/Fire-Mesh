package com.ceslab.firemesh.meshmodule.model

import com.ceslab.firemesh.myapp.*


/**
 * Created by Viet Hua on 11/23/2020.
 */



enum class VendorModelIdentifier(val assignedModelIdentifier: Int, val companyIdentifier: Int) {
    Unknown(Int.MAX_VALUE, Int.MAX_VALUE),
    NodeStatusClient(NODE_STATUS_CLIENT_ID, COMPANY_IDENTIFIER), //0x 2222 1111
    NodeStatusServer(NODE_STATUS_SERVER_ID, COMPANY_IDENTIFIER), // 0x 1111 1111
    GatewayStatusClient(GATEWAY_STATUS_CLIENT_ID, COMPANY_IDENTIFIER), // 0x 4444 1111
    GatewayStatusServer(GATEWAY_STATUS_SERVER_ID, COMPANY_IDENTIFIER) // 0x 3333 1111
}