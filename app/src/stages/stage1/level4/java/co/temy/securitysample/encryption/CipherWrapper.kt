package java.co.temy.securitysample.encryption

import android.util.Base64
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

/**
 * This class wraps [Cipher] class apis with some additional possibilities.
 */
class CipherWrapper(val transformation: String) {

    companion object {
        var TRANSFORMATION_ASYMMETRIC = "RSA/ECB/PKCS1Padding"
        var TRANSFORMATION_SYMMETRIC = "AES/CBC/PKCS7Padding"
        val IV_SEPARATOR = "]"
    }

    val cipher: Cipher = Cipher.getInstance(transformation)

    fun encrypt(data: String, key: Key?, useInitializationVector: Boolean = false): String {
        cipher.init(Cipher.ENCRYPT_MODE, key)

        var result = ""
        if (useInitializationVector) {
            val iv = cipher.iv
            val ivString = Base64.encodeToString(iv, Base64.DEFAULT)
            result = ivString + IV_SEPARATOR
        }

        val bytes = cipher.doFinal(data.toByteArray())
        result += Base64.encodeToString(bytes, Base64.DEFAULT)

        return result
    }

    fun decrypt(data: String, key: Key?, useInitializationVector: Boolean = false): String {
        var encodedString: String

        if (useInitializationVector) {
            val split = data.split(IV_SEPARATOR.toRegex())
            if (split.size != 2) throw IllegalArgumentException("Passed data is incorrect. There was no IV specified with it.")

            val ivString = split[0]
            encodedString = split[1]
            val ivSpec = IvParameterSpec(Base64.decode(ivString, Base64.DEFAULT))
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        } else {
            encodedString = data
            cipher.init(Cipher.DECRYPT_MODE, key)
        }

        val encryptedData = Base64.decode(encodedString, Base64.DEFAULT)
        val decodedData = cipher.doFinal(encryptedData)
        return String(decodedData)
    }

    fun wrapKey(keyToBeWrapped: Key, keyToWrapWith: Key?): String {
        cipher.init(Cipher.WRAP_MODE, keyToWrapWith)
        val decodedData = cipher.wrap(keyToBeWrapped)
        return Base64.encodeToString(decodedData, Base64.DEFAULT)
    }

    fun unWrapKey(wrappedKeyData: String, algorithm: String, wrappedKeyType: Int, keyToUnWrapWith: Key?): Key {
        val encryptedKeyData = Base64.decode(wrappedKeyData, Base64.DEFAULT)
        cipher.init(Cipher.UNWRAP_MODE, keyToUnWrapWith)
        return cipher.unwrap(encryptedKeyData, algorithm, wrappedKeyType)
    }
}

