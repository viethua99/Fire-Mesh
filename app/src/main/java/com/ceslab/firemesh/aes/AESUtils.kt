package com.siliconlabs.bluetoothmesh.App

import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec

//Use to encrypt + decrypt data AES

object AESUtils {
    const val ECB_ZERO_BYTE_PADDING_ALGORITHM =  "AES/ECB/ZeroBytePadding"

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class
    )
    fun decrypt(algorithm:String,keyArray:ByteArray,dataArray: ByteArray): ByteArray {
        val skeySpec = SecretKeySpec(keyArray, algorithm)
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, skeySpec)
        return cipher.doFinal(dataArray)
    }


    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class
    )
    fun encrypt(algorithm:String,keyArray:ByteArray,dataArray: ByteArray): ByteArray {
        val skeySpec = SecretKeySpec(keyArray, algorithm)
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
        return cipher.doFinal(dataArray)
    }


}