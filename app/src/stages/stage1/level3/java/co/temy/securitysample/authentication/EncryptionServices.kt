package co.temy.securitysample.authentication

import android.annotation.TargetApi
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import co.temy.securitysample.Storage
import java.co.temy.securitysample.encryption.CipherWrapper
import java.co.temy.securitysample.encryption.KeyStoreWrapper
import javax.crypto.Cipher
import javax.crypto.SecretKey

class EncryptionServices(context: Context) {

    companion object {
        val MASTER_KEY = "MASTER_KEY"
        val ALGORITHM_AES = "AES"
    }

    private val storage = Storage(context)
    private val keyStoreWrapper = KeyStoreWrapper(context)

    /*
     * Encryption Stage
     */

    fun createMasterKey(keyPassword: String? = null) {
        if (SystemServices.hasMarshmallow()) {
            createAndroidSymmetricKey()
        } else {
            createDefaultSymmetricKey()
        }
    }

    fun removeMasterKey() {
        keyStoreWrapper.removeAndroidKeyStoreKey(MASTER_KEY)
    }

    fun encrypt(data: String, keyPassword: String? = null): String {
        return if (SystemServices.hasMarshmallow()) {
            encryptWithAndroidSymmetricKey(data)
        } else {
            encryptWithDefaultSymmetricKey(data)
        }
    }

    fun decrypt(data: String, keyPassword: String? = null): String {
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
        val symmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).unWrapKey(encryptionKey, ALGORITHM_AES, Cipher.SECRET_KEY, masterKey?.private) as SecretKey
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, symmetricKey, true)
    }

    private fun decryptWithDefaultSymmetricKey(data: String): String {
        val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(MASTER_KEY)
        val encryptionKey = storage.getEncryptionKey()
        val symmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).unWrapKey(encryptionKey, ALGORITHM_AES, Cipher.SECRET_KEY, masterKey?.private) as SecretKey
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, symmetricKey, true)
    }


    /*
     * Fingerprint Stage
     */

    fun createFingerprintKey() {
    }

    fun removeFingerprintKey() {
    }

    fun prepareFingerprintCryptoObject(): FingerprintManager.CryptoObject? {
        return null
    }

    @TargetApi(23)
    fun validateFingerprintAuthentication(cryptoObject: FingerprintManager.CryptoObject): Boolean {
        return false
    }


    /*
     * Confirm Credential Stage
     */

    fun createConfirmCredentialsKey() {

    }

    fun removeConfirmCredentialsKey() {
    }

    fun validateConfirmCredentialsAuthentication(): Boolean {
        return true
    }

}