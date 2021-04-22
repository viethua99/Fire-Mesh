package com.ceslab.firemesh.meshmodule.model

/**
 * Created by Viet Hua on 11/23/2020.
 */

enum class VendorModelIdentifier(val assignedModelIdentifier: Int, val companyIdentifier: Int) {
    Unknown(Int.MAX_VALUE, Int.MAX_VALUE),
    NodeStatusClient(8738, 4369), //0x 2222 1111
    NodeStatusServer(4369, 4369), // 0x 1111 1111
    GatewayStatusClient(17476, 4369), // 0x 4444 1111
    GatewayStatusServer(13107, 4369) // 0x 3333 1111


}