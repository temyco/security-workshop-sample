package co.temy.securitysample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import co.temy.securitysample.authentication.AuthenticationDialog
import co.temy.securitysample.extentions.startSecretActivity
import co.temy.securitysample.extentions.startSignUpActivity
import kotlinx.android.synthetic.main.activity_home.*


class HomeActivity : BaseSecureActivity() {

    companion object {
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
        dialog.authenticationSuccessListener = { startSecretActivity(ADD_SECRET_REQUEST_CODE, SecretActivity.MODE_VIEW, secret) }
        dialog.passwordVerificationListener = { validatePassword(it) }
        dialog.show(supportFragmentManager, "Authentication")
    }

    /**
     * Validate password inputted from Authentication Dialog.
     */
    private fun validatePassword(inputtedPassword: String): Boolean {
        val storage = Storage(this)
        return EncryptionService(applicationContext).decrypt(storage.getPassword()) == inputtedPassword

    }

    private fun onAddPasswordClick() {
        startSecretActivity(ADD_SECRET_REQUEST_CODE)
    }
}
