package com.ceslab.firemesh.ota.model

object OTAConverters  {
    val HEX_CHARS: CharArray = "0123456789ABCDEF".toCharArray()

    // Gets value in hexadecimal system
    fun bytesToHexWhitespaceDelimited(value: ByteArray?): String {
        if (value == null) {
            return ""
        }
        val hexChars = CharArray(value.size * 3)
        var v: Int
        for (j in value.indices) {
            v = value[j].toInt() and 0xFF
            hexChars[j * 3] = HEX_CHARS[v ushr 4]
            hexChars[j * 3 + 1] = HEX_CHARS[v and 0x0F]
            hexChars[j * 3 + 2] = ' '
        }
        return String(hexChars)
    }

    // Gets value in hexadecimal system for single byte
    fun getHexValue(b: Byte): String {
        val hexChars = CharArray(2)
        val v: Int = b.toInt() and 0xFF
        hexChars[0] = HEX_CHARS[v ushr 4]
        hexChars[1] = HEX_CHARS[v and 0x0F]
        return String(hexChars)
    }

}