package com.ceslab.firemesh.meshmodule.model

/**
 * Created by Viet Hua on 11/23/2020.
 */

enum class VendorModelIdentifier(val assignedModelIdentifier: Int, val companyIdentifier: Int) {
    MyModelClient(8738, 4369), //assignedIdentifier-companyIdentifier:0x2222 1111
    MyModelServer(4369, 4369) // 0x 1111 1111
}