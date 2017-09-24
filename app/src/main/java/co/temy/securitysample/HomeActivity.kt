package co.temy.securitysample

import android.app.Activity
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import co.temy.securitysample.authentication.AuthenticationDialog
import co.temy.securitysample.authentication.AuthenticationDialog.Stage.*
import co.temy.securitysample.authentication.EncryptionServices
import co.temy.securitysample.authentication.SystemServices
import co.temy.securitysample.extentions.startSecretActivity
import co.temy.securitysample.extentions.startSignUpActivity
import kotlinx.android.synthetic.main.activity_home.*


class HomeActivity : BaseSecureActivity() {

    companion object {
        val ADD_SECRET_REQUEST_CODE = 300
    }

    private val storage: Storage by lazy(LazyThreadSafetyMode.NONE) { Storage(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val secrets = Storage(baseContext).getSecrets()

        addPasswordView.setOnClickListener { onAddPasswordClick() }
        secretsView.layoutManager = LinearLayoutManager(baseContext)
        secretsView.adapter = SecretsAdapter(secrets) { onSecretClick(it) }

        emptyView.visibility = if (secrets.isEmpty()) View.VISIBLE else View.GONE

        if (SystemServices.hasMarshmallow() && EncryptionServices(applicationContext).validateConfirmCredentialsAuthentication()) {
            Log.i("ФФ", "ALL IS GOOD")
        } else {
            Log.i("ФФ", "Need confirmation")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_SECRET_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            (secretsView.adapter as SecretsAdapter).update(Storage(baseContext).getSecrets())
            emptyView.visibility = if (secretsView.adapter.itemCount > 0) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when {
        item.itemId == R.id.menu_reset -> {
            onResetClick()
            true
        }
        else -> false
    }

    private fun onResetClick() {
        Storage(baseContext).clear()
        startSignUpActivity()
    }

    private fun onSecretClick(secret: Storage.SecretData) {
        val dialog = AuthenticationDialog()
        if (storage.isFingerprintAllowed() && systemServices.hasEnrolledFingerprints()) {
            dialog.cryptoObjectToAuthenticateWith = EncryptionServices(applicationContext).prepareFingerprintCryptoObject()
            dialog.fingerprintInvalidationListener = { onFingerprintInvalidation(it) }
            dialog.fingerprintAuthenticationSuccessListener = { validateKeyAuthentication(secret, it) }
            if (dialog.cryptoObjectToAuthenticateWith == null) dialog.stage = NEW_FINGERPRINT_ENROLLED else dialog.stage = FINGERPRINT
        } else {
            dialog.stage = PASSWORD
        }
        dialog.authenticationSuccessListener = { startSecretActivity(ADD_SECRET_REQUEST_CODE, SecretActivity.MODE_VIEW, secret) }
        dialog.passwordVerificationListener = { validatePassword(it) }
        dialog.show(supportFragmentManager, "Authentication")
    }

    private fun validateKeyAuthentication(secret: Storage.SecretData, cryptoObject: FingerprintManager.CryptoObject) {
        if (EncryptionServices(applicationContext).validateFingerprintAuthentication(cryptoObject)) {
            startSecretActivity(ADD_SECRET_REQUEST_CODE, SecretActivity.MODE_VIEW, secret)
        } else {
            onSecretClick(secret)
        }
    }

    /**
     * Validate password inputted from Authentication Dialog.
     */
    private fun validatePassword(inputtedPassword: String): Boolean {
        val storage = Storage(this)
        return EncryptionServices(applicationContext).decrypt(storage.getPassword()) == inputtedPassword

    }

    /**
     * Fingerprint was invalidated, decide what to do in this case.
     */
    private fun onFingerprintInvalidation(useInFuture: Boolean) {
        storage.saveFingerprintAllowed(useInFuture)
        if (useInFuture) {
            EncryptionServices(applicationContext).createFingerprintKey()
        }
    }

    private fun onAddPasswordClick() {
        startSecretActivity(ADD_SECRET_REQUEST_CODE)
    }
}
