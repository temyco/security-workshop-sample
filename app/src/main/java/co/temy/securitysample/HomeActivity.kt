package co.temy.securitysample

import android.app.Activity
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import co.temy.securitysample.authentication.AuthenticationDialog
import co.temy.securitysample.authentication.AuthenticationDialog.Stage.*
import co.temy.securitysample.authentication.EncryptionServices
import co.temy.securitysample.extentions.openSecuritySettings
import co.temy.securitysample.extentions.startSecretActivity
import co.temy.securitysample.extentions.startSignUpActivity
import kotlinx.android.synthetic.main.activity_home.*


class HomeActivity : BaseSecureActivity() {

    companion object {
        val ADD_SECRET_REQUEST_CODE = 300
        val AUTHENTICATION_SCREEN_CODE = 301
    }

    private val storage: Storage by lazy(LazyThreadSafetyMode.NONE) { Storage(applicationContext) }
    private var isAuthenticating = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val secrets = Storage(baseContext).getSecrets()

        addSecretView.setOnClickListener { onAddSecretClick() }
        secretsView.layoutManager = LinearLayoutManager(baseContext)
        secretsView.adapter = SecretsAdapter(secrets) { onSecretClick(it) }

        emptyView.visibility = if (secrets.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onStart() {
        super.onStart()
        if (!isAuthenticating && !EncryptionServices(applicationContext).validateConfirmCredentialsAuthentication()) {
            isAuthenticating = true
            systemServices.showAuthenticationScreen(this, AUTHENTICATION_SCREEN_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_SECRET_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            (secretsView.adapter as SecretsAdapter).update(Storage(baseContext).getSecrets())
            emptyView.visibility = if (secretsView.adapter.itemCount > 0) View.GONE else View.VISIBLE
        } else if (requestCode == AUTHENTICATION_SCREEN_CODE) {
            isAuthenticating = false
            if (resultCode != Activity.RESULT_OK) {
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        val fingerprintItem = menu.findItem(R.id.menu_fingerprint)
        if (systemServices.isFingerprintHardwareAvailable()) {
            fingerprintItem.isChecked = Storage(applicationContext).isFingerprintAllowed()
        } else {
            fingerprintItem.isVisible = false
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when {
        item.itemId == R.id.menu_reset -> {
            onResetClick()
            true
        }

        item.itemId == R.id.menu_fingerprint -> {
            onUseFingerprintClick(item)
            true
        }
        else -> false
    }

    private fun onResetClick() {
        val encryptionServices = EncryptionServices(applicationContext)
        encryptionServices.removeMasterKey()
        encryptionServices.removeFingerprintKey()
        encryptionServices.removeConfirmCredentialsKey()

        Storage(baseContext).clear()
        startSignUpActivity()
    }

    private fun onUseFingerprintClick(item: MenuItem) {
        if (!systemServices.hasEnrolledFingerprints()) {
            item.isChecked = false
            Snackbar.make(rootView, R.string.sign_up_snack_message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_up_snack_action, { openSecuritySettings() })
                    .show()
        } else {
            // Set new checkbox state
            item.isChecked = !item.isChecked

            Storage(baseContext).saveFingerprintAllowed(item.isChecked)
            if (!item.isChecked) {
                EncryptionServices(this).removeFingerprintKey()
            }
        }
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
        dialog.authenticationSuccessListener = { startSecretActivity(ADD_SECRET_REQUEST_CODE, SecretActivity.MODE_VIEW, it, secret) }
        dialog.passwordVerificationListener = { validatePassword(it) }
        dialog.show(supportFragmentManager, "Authentication")
    }

    private fun validateKeyAuthentication(secret: Storage.SecretData, cryptoObject: FingerprintManager.CryptoObject) {
        if (EncryptionServices(applicationContext).validateFingerprintAuthentication(cryptoObject)) {
            startSecretActivity(ADD_SECRET_REQUEST_CODE, SecretActivity.MODE_VIEW, secretData = secret)
        } else {
            onSecretClick(secret)
        }
    }

    /**
     * Validate password inputted from Authentication Dialog.
     */
    private fun validatePassword(inputtedPassword: String): Boolean {
        val storage = Storage(this)
        return EncryptionServices(applicationContext).decrypt(storage.getPassword(), inputtedPassword) == inputtedPassword
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

    private fun onAddSecretClick() {
        val dialog = AuthenticationDialog()
        dialog.stage = PASSWORD

        dialog.authenticationSuccessListener = { startSecretActivity(ADD_SECRET_REQUEST_CODE, password = it) }
        dialog.passwordVerificationListener = { validatePassword(it) }
        dialog.show(supportFragmentManager, "Authentication")
    }
}
