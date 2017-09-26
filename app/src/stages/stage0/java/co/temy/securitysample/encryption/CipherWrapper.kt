package java.co.temy.securitysample.encryption

import java.security.Key
import javax.crypto.Cipher

/**
 * This class wraps [Cipher] class apis with some additional possibilities.
 */
class CipherWrapper(val transformation: String) {

    /**
     * The place to keep all constants.
     */
    companion object {
    }

    /**
     * Encrypts data using the key.
     */
    fun encrypt(data: String, key: Key?): String {
        return data
    }

    /**
     * Decrypts data using the key.
     */
    fun decrypt(data: String, key: Key?): String {
        return data
    }
}

