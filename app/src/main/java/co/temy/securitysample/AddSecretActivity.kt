package co.temy.securitysample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_add_secret.*

class AddSecretActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_secret)

            secret.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    saveSecret()
                    return@OnEditorActionListener true
                }
                false
            })

            saveSecret.setOnClickListener { saveSecret() }
    }

    fun saveSecret() {
        // Store alias and secret.
        val aliasString = alias.text.toString()
        val secretString = secret.text.toString()
        var cancel = false
        var focusView: View? = null
        val storage: Storage = Storage(this)

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(aliasString)) {
            alias.error = getString(R.string.error_incorrect_alias)
            focusView = alias
            cancel = true
        } else if (storage.isSecretAliasExists(aliasString)) {
            alias.error = getString(R.string.error_duplicated_alias)
            focusView = alias
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Save secret in the encrypted storage
            storage.putSecret(Storage.SecretData(aliasString, secretString))
            finish()
        }
    }

}
