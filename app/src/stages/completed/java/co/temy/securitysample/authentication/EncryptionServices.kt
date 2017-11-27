package co.temy.securitysample.authentication

import android.annotation.TargetApi
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
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
        val CONFIRM_CREDENTIALS_KEY = "CONFIRM_CREDENTIALS_KEY"

        val KEY_VALIDATION_DATA = byteArrayOf(0, 1, 0, 1)
        val CONFIRM_CREDENTIALS_VALIDATION_DELAY = 120 // Seconds
    }

    private val keyStoreWrapper = KeyStoreWrapper(context, DEFAULT_KEY_STORE_NAME)

    /*
     * Encryption Stage
     */

    /**
     * Create and save cryptography key, to protect Secrets with.
     */
    fun createMasterKey(password: String? = null) {
        if (SystemServices.hasMarshmallow()) {
            createAndroidSymmetricKey()
        } else {
            createDefaultSymmetricKey(password ?: "")
        }
    }

    /**
     * Remove master cryptography key. May be used for re sign up functionality.
     */
    fun removeMasterKey() {
        keyStoreWrapper.removeAndroidKeyStoreKey(MASTER_KEY)
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
            keyStoreWrapper.createAndroidKeyStoreSymmetricKey(FINGERPRINT_KEY,
                    userAuthenticationRequired = true,
                    invalidatedByBiometricEnrollment = true,
                    userAuthenticationValidWhileOnBody = false)
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
                // VerifyError will be thrown on API lower then 23 if we will use unedited
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
        if (SystemServices.hasMarshmallow()) {
            keyStoreWrapper.createAndroidKeyStoreSymmetricKey(
                    CONFIRM_CREDENTIALS_KEY,
                    userAuthenticationRequired = true,
                    userAuthenticationValidityDurationSeconds = CONFIRM_CREDENTIALS_VALIDATION_DELAY)
        }
    }

    /**
     * Remove confirm credentials authentication cryptographic key.
     */
    fun removeConfirmCredentialsKey() {
        keyStoreWrapper.removeAndroidKeyStoreKey(CONFIRM_CREDENTIALS_KEY)
    }

    /**
     * @return true if confirm credential authentication is not required.
     */
    fun validateConfirmCredentialsAuthentication(): Boolean {
        if (!SystemServices.hasMarshmallow()) {
            return true
        }

        val symmetricKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(CONFIRM_CREDENTIALS_KEY)
        val cipherWrapper = CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC)

        try {
            return if (symmetricKey != null) {
                cipherWrapper.encrypt(KEY_VALIDATION_DATA.toString(), symmetricKey)
                true
            } else false
        } catch (e: Throwable) {
            // VerifyError is will be thrown on API lower then 23 if we will use unedited
            // class reference directly in catch block
            if (e is UserNotAuthenticatedException || e is KeyPermanentlyInvalidatedException) {
                // User is not authenticated or the lock screen has been disabled or reset
                return false
            } else if (e is InvalidKeyException) {
                // Confirm Credentials key was not generated
                return false
            }
            throw e
        }
    }

}