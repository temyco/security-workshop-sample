package co.temy.securitysample.authentication

import android.annotation.TargetApi
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.security.keystore.KeyPermanentlyInvalidatedException
import co.temy.securitysample.Storage
import java.co.temy.securitysample.encryption.CipherWrapper
import java.co.temy.securitysample.encryption.KeyStoreWrapper
import java.security.InvalidKeyException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException

class EncryptionServices(context: Context) {

    /**
     * The place to keep all constants.
     */
    companion object {
        val DEFAULT_KEY_STORE_NAME = "default_keystore"
        val MASTER_KEY = "MASTER_KEY"
        val FINGERPRINT_KEY = "FINGERPRINT_KEY"
        val KEY_VALIDATION_DATA = byteArrayOf(0, 1, 0, 1)
    }

    private val storage = Storage(context)
    private val keyStoreWrapper = KeyStoreWrapper(context, DEFAULT_KEY_STORE_NAME)

    /*
     * Encryption Stage
     */

    /**
     * Create and save cryptography key, to protect Secrets with.
     */
    fun createMasterKey(keyPassword: String? = null) {
        if (SystemServices.hasMarshmallow()) {
            createAndroidSymmetricKey()
        } else {
            createDefaultSymmetricKey(keyPassword ?: "")
        }
    }

    /**
     * Remove master cryptography key. May be used for re sign up functionality.
     */
    fun removeMasterKey() {
    }

    /**
     * Encrypt user password and Secrets with created master key.
     */
    fun encrypt(data: String, keyPassword: String? = null): String {
        return if (SystemServices.hasMarshmallow()) {
            encryptWithAndroidSymmetricKey(data)
        } else {
            encryptWithDefaultSymmetricKey(data, keyPassword ?: "")
        }
    }

    /**
     * Decrypt user password and Secrets with created master key.
     */
    fun decrypt(data: String, keyPassword: String? = null): String {
        return if (SystemServices.hasMarshmallow()) {
            decryptWithAndroidSymmetricKey(data)
        } else {
            decryptWithDefaultSymmetricKey(data, keyPassword ?: "")
        }
    }

    private fun createAndroidSymmetricKey() {
        keyStoreWrapper.createAndroidKeyStoreSymmetricKey(MASTER_KEY)
    }

    private fun encryptWithAndroidSymmetricKey(data: String): String {
        val masterKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY)
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, masterKey, true)
    }

    private fun decryptWithAndroidSymmetricKey(data: String): String {
        val masterKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY)
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, masterKey, true)
    }

    private fun createDefaultSymmetricKey(password: String) {
        keyStoreWrapper.createDefaultKeyStoreSymmetricKey(MASTER_KEY, password)
    }

    private fun encryptWithDefaultSymmetricKey(data: String, keyPassword: String): String {
        val masterKey = keyStoreWrapper.getDefaultKeyStoreSymmetricKey(MASTER_KEY, keyPassword)
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, masterKey, true)
    }

    private fun decryptWithDefaultSymmetricKey(data: String, keyPassword: String): String {
        val masterKey = keyStoreWrapper.getDefaultKeyStoreSymmetricKey(MASTER_KEY, keyPassword)
        return masterKey?.let { CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, masterKey, true) } ?: ""
    }


    /*
     * Fingerprint Stage
     */

    /**
     * Create and save cryptography key, that will be used for fingerprint authentication.
     */
    fun createFingerprintKey() {
        if (SystemServices.hasMarshmallow()) {
            keyStoreWrapper.createAndroidKeyStoreSymmetricKey(FINGERPRINT_KEY, true, true)
        }
    }

    /**
     * Remove fingerprint authentication cryptographic key.
     */
    fun removeFingerprintKey() {
        if (SystemServices.hasMarshmallow()) {
            keyStoreWrapper.removeAndroidKeyStoreKey(FINGERPRINT_KEY)
        }
    }

    /**
     * @return initialized crypto object or null if fingerprint key was invalidated or not created yet.
     */
    fun prepareFingerprintCryptoObject(): FingerprintManager.CryptoObject? {
        return if (SystemServices.hasMarshmallow()) {
            try {
                val symmetricKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(FINGERPRINT_KEY)
                val cipher = CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).cipher
                cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)
                FingerprintManager.CryptoObject(cipher)
            } catch (e: Throwable) {
                // VerifyError is will be thrown on API lower then 23 if we will use unedited
                // class reference directly in catch block
                if (e is KeyPermanentlyInvalidatedException || e is IllegalBlockSizeException) {
                    return null
                } else if (e is InvalidKeyException) {
                    // Fingerprint key was not generated
                    return null
                }
                throw e
            }
        } else null
    }

    /**
     * @return true if cryptoObject was initialized successfully and key was not invalidated during authentication.
     */
    @TargetApi(23)
    fun validateFingerprintAuthentication(cryptoObject: FingerprintManager.CryptoObject): Boolean {
        try {
            cryptoObject.cipher.doFinal(KEY_VALIDATION_DATA)
            return true
        } catch (e: Throwable) {
            // VerifyError is will be thrown on API lower then 23 if we will use unedited
            // class reference directly in catch block
            if (e is KeyPermanentlyInvalidatedException || e is IllegalBlockSizeException) {
                return false
            }
            throw e
        }
    }

    /*
     * Confirm Credential Stage
     */

    /**
     * Create and save cryptography key, that will be used for confirm credentials authentication.
     */
    fun createConfirmCredentialsKey() {

    }

    /**
     * Remove confirm credentials authentication cryptographic key.
     */
    fun removeConfirmCredentialsKey() {
    }

    /**
     * @return true if confirm credential authentication is not required.
     */
    fun validateConfirmCredentialsAuthentication(): Boolean {
        return true
    }

}