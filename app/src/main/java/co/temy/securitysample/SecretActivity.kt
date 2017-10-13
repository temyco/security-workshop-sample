package co.temy.securitysample

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import co.temy.securitysample.authentication.EncryptionServices
import co.temy.securitysample.extentions.hideKeyboard
import co.temy.securitysample.extentions.showKeyboard
import kotlinx.android.synthetic.main.activity_add_secret.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class SecretActivity : AppCompatActivity() {

    companion object {
        val MODE_CREATE = 1
        val MODE_VIEW = 2
        val MODE_EDIT = 3
    }

    private val dateFormatter = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    private var mode: Int = MODE_CREATE
    private var password: String? = null
    private var secret: Storage.SecretData? = null
    private var aliasEditBg: Drawable? = null
    private var secretEditBg: Drawable? = null
    private lateinit var menu: Menu
    private lateinit var storage: Storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_secret)

        storage = Storage(applicationContext)
        mode = intent.getIntExtra("mode", MODE_CREATE)
        password = intent.getStringExtra("password")
        secret = intent.getSerializableExtra("secret").let { it as Storage.SecretData? }

        initViewForMode()
        secretEditView.setOnEditorActionListener({ _, id, _ -> onEditorAction(id) })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_secret, menu)
        initMenuForMode()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when {
        item.itemId == R.id.menu_save_secret -> {
            onSaveClick()
            true
        }
        item.itemId == R.id.menu_edit_secret -> {
            changeMode(MODE_EDIT)
            true
        }
        item.itemId == R.id.menu_delete_secret -> {
            onDeleteClick()
            true
        }
        item.itemId == android.R.id.home -> {
            secretRootView.focusedChild?.hideKeyboard()
            super.onOptionsItemSelected(item)
        }
        else -> false
    }

    private fun changeMode(mode: Int) {
        this.mode = mode
        initMenuForMode()
        initViewForMode()
    }

    private fun initViewForMode() {
        when (mode) {
            MODE_VIEW -> {
                title = null

                secretRootView.focusedChild?.hideKeyboard()
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

                aliasEditView.setText(secret?.alias)
                secretEditView.setText(secret?.let { decryptSecret(it.secret) } ?: "")

                if (secret?.createDate == secret?.updateDate) {
                    dateView.text = getString(R.string.secret_created_date, dateFormatter.format(secret?.createDate))
                } else {
                    dateView.text = getString(R.string.secret_updated_date, dateFormatter.format(secret?.updateDate))
                }

                dateView.visibility = View.VISIBLE
                secretDividerView.visibility = View.VISIBLE

                aliasEditBg = aliasEditView.background
                aliasInputView.isHintEnabled = false
                aliasInputView.isEnabled = false
                aliasEditView.isEnabled = false
                aliasEditView.background = null

                secretEditBg = secretEditView.background
                secretInputView.isHintEnabled = false
                secretInputView.isCounterEnabled = false
                secretInputView.isEnabled = false
                secretEditView.isEnabled = false
                secretEditView.background = null
            }
            MODE_EDIT -> {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

                dateView.visibility = View.GONE
                secretDividerView.visibility = View.GONE

                aliasEditView.requestFocus()
                aliasEditView.showKeyboard()
                aliasEditView.setSelection(aliasEditView.text.length)
                secretEditView.setSelection(secretEditView.text.length)

                aliasInputView.isEnabled = true
                aliasEditView.isEnabled = true
                aliasEditView.background = aliasEditBg

                secretInputView.isCounterEnabled = true
                secretInputView.isEnabled = true
                secretEditView.isEnabled = true
                secretEditView.background = secretEditBg
            }
        }
    }

    private fun initMenuForMode() {
        when (mode) {
            MODE_CREATE -> {
                menu.findItem(R.id.menu_delete_secret).isVisible = false
                menu.findItem(R.id.menu_edit_secret).isVisible = false
            }

            MODE_VIEW -> {
                menu.findItem(R.id.menu_save_secret).isVisible = false
                menu.findItem(R.id.menu_edit_secret).isVisible = true
            }

            MODE_EDIT -> {
                menu.findItem(R.id.menu_save_secret).isVisible = true
                menu.findItem(R.id.menu_edit_secret).isVisible = false
            }
        }
    }

    private fun onEditorAction(id: Int): Boolean {
        return if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
            onSaveClick()
            true
        } else false
    }

    private fun onSaveClick() {
        when (mode) {
            MODE_CREATE -> validateInput { alias, secret -> saveSecret(alias, secret) }
            MODE_EDIT -> validateInput { alias, secret -> editSecret(alias, secret) }
        }
    }

    private fun onDeleteClick() {
        secret?.alias?.let { storage.removeSecret(it) }
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun validateInput(onSuccess: (String, String) -> Unit) {
        aliasInputView.error = null
        secretInputView.error = null

        val aliasString = aliasEditView.text.toString()
        val secretString = secretEditView.text.toString()
        var cancel = false
        var focusView: View? = null

        when {
            TextUtils.isEmpty(aliasString) -> {
                aliasInputView.error = getString(R.string.error_incorrect_alias)
                focusView = aliasEditView
                cancel = true
            }
            secret?.alias != aliasString && storage.hasSecret(aliasString) -> {
                aliasInputView.error = getString(R.string.error_duplicated_alias)
                focusView = aliasEditView
                cancel = true
            }
            TextUtils.isEmpty(secretString) -> {
                secretInputView.error = getString(R.string.secret_error_empty_secret)
                focusView = secretEditView
                cancel = true
            }
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            focusView?.hideKeyboard()
            onSuccess(aliasString, secretString)
        }
    }

    private fun editSecret(newAlias: String, secret: String) {
        this.secret?.let {
            storage.removeSecret(it.alias)
            val newSecret = createSecretData(newAlias, secret, it.createDate)
            storage.saveSecret(newSecret)
            this.secret = newSecret
        }
        setResult(Activity.RESULT_OK)
        changeMode(MODE_VIEW)
    }

    private fun saveSecret(alias: String, secret: String) {
        storage.saveSecret(createSecretData(alias, secret, Date()))
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun createSecretData(alias: String, secret: String, createDate: Date): Storage.SecretData {
        val encryptedSecret = encryptSecret(secret)
        logi("Original secret is: $secret")
        logi("Saved secret is: $encryptedSecret")
        return Storage.SecretData(alias.capitalize(), encryptedSecret, createDate, updateDate = Date())
    }

    /**
     * Encrypt secret before saving it.
     */
    private fun encryptSecret(secret: String): String {
        return EncryptionServices(applicationContext).encrypt(secret, password)
    }

    /**
     * Decrypt secret before showing it.
     */
    private fun decryptSecret(secret: String): String {
        return EncryptionServices(applicationContext).decrypt(secret, password)
    }
}
