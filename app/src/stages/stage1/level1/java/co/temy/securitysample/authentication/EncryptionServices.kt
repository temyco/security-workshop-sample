package co.temy.securitysample.authentication

import android.annotation.TargetApi
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import java.co.temy.securitysample.encryption.CipherWrapper
import java.co.temy.securitysample.encryption.KeyStoreWrapper

class EncryptionServices(context: Context) {

    companion object {
        val MASTER_KEY = "MASTER_KEY"
    }

    private val keyStoreWrapper = KeyStoreWrapper(context)

    /*
     * Encryption Stage
     */

    fun createMasterKey(keyPassword: String? = null) {
        keyStoreWrapper.createAndroidKeyStoreAsymmetricKey(MASTER_KEY)
    }

    fun removeMasterKey() {
        keyStoreWrapper.removeAndroidKeyStoreKey(MASTER_KEY)
    }

    fun encrypt(data: String, keyPassword: String? = null): String {
        val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(MASTER_KEY)
        return CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).encrypt(data, masterKey?.public)
    }

    fun decrypt(data: String, keyPassword: String? = null): String {
        val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(MASTER_KEY)
        return CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).decrypt(data, masterKey?.private)
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