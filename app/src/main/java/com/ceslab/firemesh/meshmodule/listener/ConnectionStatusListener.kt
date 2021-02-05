package com.ceslab.firemesh.meshmodule.listener

import com.siliconlab.bluetoothmesh.adk.ErrorType

/**
 * Created by Viet Hua on 11/23/2020.
 */

interface ConnectionStatusListener {
    fun connecting()

    fun connected()

    fun disconnected()



}