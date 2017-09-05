package co.temy.securitysample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import co.temy.securitysample.encryption.CipherWrapper
import co.temy.securitysample.encryption.KeyStoreWrapper
import co.temy.securitysample.extentions.startSecretActivity
import co.temy.securitysample.system.SecretsAdapter
import kotlinx.android.synthetic.main.activity_home.*


class HomeActivity : BaseSecureActivity() {

    companion object {
        val TEST_PASSWORD = "TEST_PASSWORD"
        val ADD_SECRET_REQUEST_CODE = 300
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val secrets = Storage(baseContext).getSecrets()

        addPasswordView.setOnClickListener { onAddPasswordClick() }
        secretsView.layoutManager = LinearLayoutManager(baseContext)
        secretsView.adapter = SecretsAdapter(secrets) { onSecretClick(it) }

        emptyView.visibility = if (secrets.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_SECRET_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            (secretsView.adapter as SecretsAdapter).update(Storage(baseContext).getSecrets())
            emptyView.visibility = View.GONE
        }
    }

    private fun onSecretClick(secret: Storage.SecretData) {
        startSecretActivity(ADD_SECRET_REQUEST_CODE, SecretActivity.MODE_VIEW, secret)
    }

    private fun onAddPasswordClick() {
        startSecretActivity(ADD_SECRET_REQUEST_CODE)
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
