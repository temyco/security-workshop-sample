package java.co.temy.securitysample.encryption

import android.util.Base64
import java.security.Key
import javax.crypto.Cipher

/**
 * This class wraps [Cipher] class apis with some additional possibilities.
 */
class CipherWrapper(val transformation: String) {

    companion object {
        var TRANSFORMATION_ASYMMETRIC = "RSA/ECB/PKCS1Padding"
    }

    val cipher: Cipher = Cipher.getInstance(transformation)

    fun encrypt(data: String, key: Key?): String {
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val bytes = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    fun decrypt(data: String, key: Key?): String {
        cipher.init(Cipher.DECRYPT_MODE, key)
        val encryptedData = Base64.decode(data, Base64.DEFAULT)
        val decodedData = cipher.doFinal(encryptedData)
        return String(decodedData)
    }
}

