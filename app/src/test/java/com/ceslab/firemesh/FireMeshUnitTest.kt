package com.ceslab.firemesh

import com.ceslab.firemesh.ota.utils.Converters
import org.junit.Assert
import org.junit.Test

class FireMeshUnitTest {

    @Test
    fun data_convert_1() {
        val byteArray = byteArrayOf(0x20,0x03)
        val test = "%04x".format(8195)
        Assert.assertEquals("0a0b", test)
    }
}