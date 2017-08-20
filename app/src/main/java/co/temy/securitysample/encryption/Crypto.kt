package co.temy.securitysample.encryption

import android.content.Context
import java.security.KeyStore
import java.security.KeyStoreException

class Crypto(context: Context) {

    private val keyStore: KeyStore

    init {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to get an instance of KeyStore", e)
        }
    }

    public fun getAllKeyAlieases(): List<String> {
        return keyStore.aliases().toList()
    }

}