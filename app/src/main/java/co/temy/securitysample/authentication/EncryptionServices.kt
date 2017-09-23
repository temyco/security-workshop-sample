package co.temy.securitysample.authentication

import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.security.keystore.KeyPermanentlyInvalidatedException
import co.temy.securitysample.Storage
import co.temy.securitysample.encryption.CipherWrapper
import co.temy.securitysample.encryption.KeyStoreWrapper
import javax.crypto.Cipher
import javax.crypto.SecretKey

class EncryptionServices(context: Context) {

    companion object {
        val MASTER_KEY = "MASTER_KEY"
        val FINGERPRINT_KEY = "FINGERPRINT_KEY"
        val CONFIRM_CREDENTIALS_KEY = "CONFIRM_CREDENTIALS_KEY"
        val ALGORITHM_AES = "AES"
    }

    private val storage = Storage(context)
    private val keyStoreWrapper = KeyStoreWrapper(context)

    fun createMasterKey() {
        if (SystemServices.hasMarshmallow()) {
            createAndroidSymmetricKey()
        } else {
            createDefaultSymmetricKey()
        }
    }

    fun createFingerprintKey() {
        if (SystemServices.hasMarshmallow()) {
            keyStoreWrapper.createAndroidKeyStoreSymmetricKey(FINGERPRINT_KEY, true, true)
        }
    }

    fun createConfirmCredentialsKey() {
        if (SystemServices.hasMarshmallow()) {
        }
    }

    /**
     * @return initialized crypto object or null if fingerprint key was invalidated.
     */
    fun prepareFingerprintCryptoObject(): FingerprintManager.CryptoObject? {
        return if (SystemServices.hasMarshmallow()) {
            try {
                val symmetricKey = keyStoreWrapper.getSymmetricKey(FINGERPRINT_KEY)
                val cipher = CipherWrapper.createCipher(CipherWrapper.TRANSFORMATION_SYMMETRIC)
                cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)
                FingerprintManager.CryptoObject(cipher)
            } catch (e: Throwable) {
                // VerifyError is will be thrown on API lower then 23 if we will use unedited
                // class reference directly in catch block
                if (e is KeyPermanentlyInvalidatedException) {
                    return null
                }
                throw e
            }
        } else null
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
        val masterKey = keyStoreWrapper.getSymmetricKey(MASTER_KEY)
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, masterKey)
    }

    private fun decryptWithAndroidSymmetricKey(data: String): String {
        val masterKey = keyStoreWrapper.getSymmetricKey(MASTER_KEY)
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, masterKey)
    }

    private fun createDefaultSymmetricKey() {
        val symmetricKey = keyStoreWrapper.generateDefaultSymmetricKey()
        val masterKey = keyStoreWrapper.createAndroidKeyStoreAsymmetricKey(MASTER_KEY)
        val encryptedSymmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).wrapKey(symmetricKey, masterKey.public)
        storage.saveEncryptionKey(encryptedSymmetricKey)
    }

    private fun encryptWithDefaultSymmetricKey(data: String): String {
        val masterKey = keyStoreWrapper.getAsymmetricKeyPair(MASTER_KEY)
        val encryptionKey = storage.getEncryptionKey()
        val symmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).unWrapKey(encryptionKey, ALGORITHM_AES, Cipher.SECRET_KEY, masterKey.private) as SecretKey
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, symmetricKey)
    }

    private fun decryptWithDefaultSymmetricKey(data: String): String {
        val masterKey = keyStoreWrapper.getAsymmetricKeyPair(MASTER_KEY)
        val encryptionKey = storage.getEncryptionKey()
        val symmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).unWrapKey(encryptionKey, ALGORITHM_AES, Cipher.SECRET_KEY, masterKey.private) as SecretKey
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, symmetricKey)
    }

    class CryptoHolder(val cryptoObject: FingerprintManager.CryptoObject)
}