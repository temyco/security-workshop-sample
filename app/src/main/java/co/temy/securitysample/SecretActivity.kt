package co.temy.securitysample

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.activity_add_secret.*
import java.util.*

class SecretActivity : AppCompatActivity() {

    companion object {
        val MODE_CREATE = 1
        val MODE_VIEW = 2
        val MODE_EDIT = 3
    }

    private var mode: Int = MODE_CREATE
    private var secret: Storage.SecretData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_secret)

        mode = intent.getIntExtra("mode", MODE_CREATE)
        secret = intent.getSerializableExtra("secret").let { it as Storage.SecretData? }

        secretView.setOnEditorActionListener({ _, id, _ -> onEditorAction(id) })
        saveSecret.setOnClickListener { saveSecret() }
    }

    private fun onEditorAction(id: Int): Boolean {
        return if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
            saveSecret()
            true
        } else false
    }

    private fun saveSecret() {
        // Store alias and secret.
        val aliasString = alias.text.toString()
        val secretString = secretView.text.toString()
        var cancel = false
        var focusView: View? = null
        val storage = Storage(this)

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(aliasString)) {
            alias.error = getString(R.string.error_incorrect_alias)
            focusView = alias
            cancel = true
        } else if (storage.hasSecret(aliasString)) {
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
            storage.saveSecret(Storage.SecretData(aliasString, secretString, Date()))
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

}
