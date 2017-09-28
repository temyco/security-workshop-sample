package co.temy.securitysample.authentication

import android.annotation.TargetApi
import android.content.Context
import android.hardware.fingerprint.FingerprintManager

class EncryptionServices(context: Context) {

    companion object {

    }

    /*
     * Encryption Stage
     */

    fun createMasterKey(keyPassword: String? = null) {
    }

    fun removeMasterKey() {
    }

    fun encrypt(data: String, keyPassword: String? = null): String {
        return data
    }

    fun decrypt(data: String, keyPassword: String? = null): String {
        return data
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