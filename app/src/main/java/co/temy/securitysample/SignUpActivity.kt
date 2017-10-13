package co.temy.securitysample

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import co.temy.securitysample.authentication.EncryptionServices
import co.temy.securitysample.authentication.SystemServices
import co.temy.securitysample.extentions.hideKeyboard
import co.temy.securitysample.extentions.openSecuritySettings
import co.temy.securitysample.extentions.startHomeActivity
import kotlinx.android.synthetic.main.activity_sign_up.*

/**
 * Sign up with password screen.
 */
class SignUpActivity : BaseSecureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        initViews()
    }

    private fun initViews() {
        confirmPasswordView.setOnEditorActionListener({ _, id, _ -> onEditorActionClick(id) })
        doneView.setOnClickListener { attemptToSignUp() }

        if (systemServices.isFingerprintHardwareAvailable()) {
            allowFingerprintView.visibility = View.VISIBLE
        }
        allowFingerprintView.setOnCheckedChangeListener { _, checked -> onAllowFingerprint(checked) }
    }

    private fun onAllowFingerprint(checked: Boolean) {
        if (checked && !systemServices.hasEnrolledFingerprints()) {
            allowFingerprintView.isChecked = false
            Snackbar.make(signUpRootView, R.string.sign_up_snack_message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_up_snack_action, { openSecuritySettings() })
                    .show()
        }
    }

    private fun onEditorActionClick(id: Int): Boolean = when (id) {
        EditorInfo.IME_ACTION_DONE, EditorInfo.IME_NULL -> {
            attemptToSignUp()
            true
        }
        else -> false
    }

    /**
     * Attempts to sign up with password specified by the sing up form.
     * If there are form errors errors are presented and no actual sing up attempt is made.
     */
    private fun attemptToSignUp() {
        passwordHolderView.error = null
        confirmPasswordHolderView.error = null

        val passwordString = passwordView.text.toString()
        val confirmPasswordString = confirmPasswordView.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!isPasswordValid(passwordString)) {
            passwordHolderView.error = getString(R.string.sign_up_error_invalid_password)
            focusView = passwordView
            cancel = true
        } else if (passwordString != confirmPasswordString) {
            confirmPasswordHolderView.error = getString(R.string.sign_up_error_incorrect_password)
            focusView = confirmPasswordView
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            createKeys(passwordString, allowFingerprintView.isChecked)

            with(Storage(this)) {
                val encryptedPassword = EncryptionServices(applicationContext).encrypt(passwordString, passwordString)
                logi("Original password is: $passwordString")
                logi("Saved password is: $encryptedPassword")

                savePassword(encryptedPassword)
                saveFingerprintAllowed(allowFingerprintView.isChecked)
            }

            focusView?.hideKeyboard()
            startHomeActivity()
        }
    }

    /**
     * Create master, fingerprint and confirm credentials keys.
     */
    private fun createKeys(password: String, isFingerprintAllowed: Boolean) {
        val encryptionService = EncryptionServices(applicationContext)
        encryptionService.createMasterKey(password)

        if (SystemServices.hasMarshmallow()) {
            if (isFingerprintAllowed && systemServices.hasEnrolledFingerprints()) {
                encryptionService.createFingerprintKey()
            }
            encryptionService.createConfirmCredentialsKey()
        }
    }

    private fun isPasswordValid(password: String) = !TextUtils.isEmpty(password) && password.length >= 6
}
