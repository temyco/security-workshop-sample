package co.temy.securitysample.authentication

import android.annotation.TargetApi
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import co.temy.securitysample.Storage
import co.temy.securitysample.encryption.CipherWrapper
import co.temy.securitysample.encryption.KeyStoreWrapper
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.SecretKey

class EncryptionServices(context: Context) {

    companion object {
        val MASTER_KEY = "MASTER_KEY"
        val FINGERPRINT_KEY = "FINGERPRINT_KEY"
        val CONFIRM_CREDENTIALS_KEY = "CONFIRM_CREDENTIALS_KEY"
        val ALGORITHM_AES = "AES"

        val KEY_VALIDATION_DATA = byteArrayOf(0, 1, 0, 1)
        val CONFIRM_CREDENTIALS_VALIDATION_DELAY = 60 // Seconds
    }

    private val storage = Storage(context)
    private val keyStoreWrapper = KeyStoreWrapper(context)

    /*
     * Encryption Stage
     */

    fun createMasterKey() {
        if (SystemServices.hasMarshmallow()) {
            createAndroidSymmetricKey()
        } else {
            createDefaultSymmetricKey()
        }
    }

    fun encrypt(data: String): String {
        return if (SystemServices.hasMarshmallow()) {
            encryptWithAndroidSymmetricKey(data)
        } else {
            encryptWithDefaultSymmetricKey(data)
        }
    }

    fun decrypt(data: String): String {
        return if (SystemServices.hasMarshmallow()) {
            decryptWithAndroidSymmetricKey(data)
        } else {
            decryptWithDefaultSymmetricKey(data)
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

    private fun createDefaultSymmetricKey() {
        val symmetricKey = keyStoreWrapper.generateDefaultSymmetricKey()
        val masterKey = keyStoreWrapper.createAndroidKeyStoreAsymmetricKey(MASTER_KEY)
        val encryptedSymmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).wrapKey(symmetricKey, masterKey.public)
        storage.saveEncryptionKey(encryptedSymmetricKey)
    }

    private fun encryptWithDefaultSymmetricKey(data: String): String {
        val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(MASTER_KEY)
        val encryptionKey = storage.getEncryptionKey()
        val symmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).unWrapKey(encryptionKey, ALGORITHM_AES, Cipher.SECRET_KEY, masterKey.private) as SecretKey
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, symmetricKey, true)
    }

    private fun decryptWithDefaultSymmetricKey(data: String): String {
        val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(MASTER_KEY)
        val encryptionKey = storage.getEncryptionKey()
        val symmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).unWrapKey(encryptionKey, ALGORITHM_AES, Cipher.SECRET_KEY, masterKey.private) as SecretKey
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, symmetricKey, true)
    }


    /*
     * Fingerprint Stage
     */

    fun createFingerprintKey() {
        if (SystemServices.hasMarshmallow()) {
            keyStoreWrapper.createAndroidKeyStoreSymmetricKey(FINGERPRINT_KEY, true, true)
        }
    }

    /**
     * @return initialized crypto object or null if fingerprint key was invalidated.
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
                }
                throw e
            }
        } else null
    }

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

    fun createConfirmCredentialsKey() {
        if (SystemServices.hasMarshmallow()) {
            keyStoreWrapper.createAndroidKeyStoreSymmetricKey(
                    CONFIRM_CREDENTIALS_KEY,
                    userAuthenticationRequired = true,
                    userAuthenticationValidityDurationSeconds = CONFIRM_CREDENTIALS_VALIDATION_DELAY)
        }
    }


    fun validateConfirmCredentialsAuthentication(): Boolean {
        val symmetricKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(CONFIRM_CREDENTIALS_KEY)
        val cipherWrapper = CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC)

        try {
            cipherWrapper.encrypt(KEY_VALIDATION_DATA.toString(), symmetricKey)
            return true
        } catch (e: Throwable) {
            // VerifyError is will be thrown on API lower then 23 if we will use unedited
            // class reference directly in catch block
            if (e is UserNotAuthenticatedException || e is KeyPermanentlyInvalidatedException) {
                // User is not authenticated or the lock screen has been disabled or reset
                return false
            }
            throw e
        }
    }

}