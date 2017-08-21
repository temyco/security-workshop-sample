package co.temy.securitysample;

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.temy.securitysample.encryption.CipherWrapper
import co.temy.securitysample.encryption.KeyStoreWrapper

class EncryptionFragment : Fragment() {

    companion object {
        val TEST_PASSWORD = "TEST_PASSWORD"
    }

    private var keyNumber = 0

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_encryption, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view?.findViewById<View>(R.id.btnAliases)?.setOnClickListener { printAliases() }
        view?.findViewById<View>(R.id.btnCreateSyncKey)?.setOnClickListener { createKey() }
        view?.findViewById<View>(R.id.btnEncrypt)?.setOnClickListener { testEncryption() }
    }

    private fun printAliases() {
        val keyStore = KeyStoreWrapper()
        keyStore.getAllKeyAliases()
                .sortedBy { it.creationDate }
                .forEach { Log.i("EncryptionFragment", "${it} \n") }
    }

    private fun createKey() {
        val keyStore = KeyStoreWrapper()
        keyStore.createSymmetricKey("Test-${++keyNumber}", false, false)
    }

    private fun testEncryption() {
        val keyStore = KeyStoreWrapper()
        val symmetricKey = keyStore.createSymmetricKey(alias = TEST_PASSWORD, userAuthenticationRequired = false, invalidatedByBiometricEnrollment = false)

        val cipher = CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC)

        if (symmetricKey != null) {

            val encryptedData = cipher.encrypt(TEST_PASSWORD, symmetricKey)
            val decryptedData = cipher.decrypt(encryptedData, symmetricKey)

            Log.i("EncryptionFragment", "Input Value : ${TEST_PASSWORD}")
            Log.i("EncryptionFragment", "Encrypted Value : $encryptedData")
            Log.i("EncryptionFragment", "Decrypted Value : ${decryptedData}")
        }
    }
}
