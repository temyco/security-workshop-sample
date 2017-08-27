package co.temy.securitysample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import co.temy.securitysample.encryption.CipherWrapper
import co.temy.securitysample.encryption.KeyStoreWrapper
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : BaseSecureActivity() {

    companion object {
        val TEST_PASSWORD = "TEST_PASSWORD"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        addPasswordView.setOnClickListener { onAddPasswordClick() }
    }

    private fun onAddPasswordClick() {
        val intent = Intent(this, AddSecretActivity::class.java)
        startActivity(intent)
    }

    private fun printAliases() {
        val keyStore = KeyStoreWrapper()
        keyStore.getAllKeyAliases()
                .sortedBy { it.creationDate }
                .forEach { Log.i("EncryptionFragment", "${it} \n") }
    }

    private var keyNumber = 0

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
