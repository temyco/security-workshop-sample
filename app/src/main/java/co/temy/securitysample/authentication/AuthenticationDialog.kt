package co.temy.securitysample.authentication

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import co.temy.securitysample.R
import co.temy.securitysample.SystemServices
import co.temy.securitysample.extentions.showKeyboard
import kotlinx.android.synthetic.main.dialog_fingerprint_backup.*
import kotlinx.android.synthetic.main.dialog_fingerprint_container.*
import kotlinx.android.synthetic.main.dialog_fingerprint_content.*

class AuthenticationDialog : AppCompatDialogFragment(), FingerprintUiHelper.Callback {

    var passwordVerificationListener: ((password: String) -> Boolean)? = null
    var authenticationSuccessListener: (() -> Unit)? = null

    var fingerprintAuthenticationSuccessListener: ((cryptoObject: FingerprintManager.CryptoObject?) -> Unit)? = null
    var fingerprintInvalidationListener: ((invalidatedByBiometricEnrollment: Boolean) -> Unit)? = null

    var stage = Stage.FINGERPRINT

    /**
     * The crypto object to be passed in when authenticating with fingerprint.
     */
    var cryptoObject: FingerprintManager.CryptoObject? = null

    private lateinit var mActivity: Activity
    private lateinit var mSharedPreferences: SharedPreferences

    private var mFingerprintUiHelper: FingerprintUiHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        retainInstance = true
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AuthenticationDialog)
    }

    override fun onResume() {
        super.onResume()
        if (stage == Stage.FINGERPRINT) mFingerprintUiHelper?.startListening(cryptoObject)
    }

    override fun onPause() {
        super.onPause()
        mFingerprintUiHelper?.stopListening()
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = activity
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.dialog_fingerprint_container, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog.setTitle(getString(R.string.authentication_title))
        cancelButtonView.setOnClickListener { dismiss() }
        secondButtonView.setOnClickListener { if (stage == Stage.FINGERPRINT) goToBackup() else verifyPassword() }
        passwordView.setOnEditorActionListener { _, actionId, _ -> onEditorAction(actionId) }

        if (SystemServices.hasMarshmallow()) {
            mFingerprintUiHelper = FingerprintUiHelper(SystemServices(context.applicationContext), fingerprintIconView, fingerprintStatusView, this)
        }

        updateStage()

        // If fingerprint authentication is not available, switch immediately to the backup (password) screen.
        if (mFingerprintUiHelper?.isFingerprintAuthAvailable != true) {
            goToBackup()
        }
    }

    override fun onAuthenticated() {
        // Callback from FingerprintUiHelper. Let the activity know that authentication was successful.
        fingerprintAuthenticationSuccessListener?.invoke(cryptoObject)
        dismiss()
    }

    override fun onError() {
        goToBackup()
    }

    private fun onEditorAction(actionId: Int): Boolean {
        return if (actionId == EditorInfo.IME_ACTION_GO) {
            verifyPassword()
            true
        } else false
    }

    private fun updateStage() {
        when (stage) {
            Stage.FINGERPRINT -> showFingerprintStage()
            Stage.NEW_FINGERPRINT_ENROLLED, Stage.PASSWORD -> showBackupStage()
        }
    }

    private fun showFingerprintStage() {
        cancelButtonView.setText(R.string.authentication_cancel)
        secondButtonView.setText(R.string.authentication_use_password)
        fingerprintContainerView.visibility = View.VISIBLE
        backupContainerView.visibility = View.GONE

    }

    private fun showBackupStage() {
        cancelButtonView.setText(R.string.authentication_cancel)
        secondButtonView.setText(R.string.authentication_ok)
        fingerprintContainerView.visibility = View.GONE
        backupContainerView.visibility = View.VISIBLE
        if (stage == Stage.NEW_FINGERPRINT_ENROLLED) {
            passwordDescriptionView.visibility = View.GONE
            fingerprintEnrolledView.visibility = View.VISIBLE
            useFingerprintInFutureView.visibility = View.VISIBLE
        }
    }

    /**
     * Switches to backup (password) screen. This either can happen when fingerprint is not
     * available or the user chooses to use the password authentication method by pressing the
     * button. This can also happen when the user had too many fingerprint attempts.
     */
    private fun goToBackup() {
        stage = Stage.PASSWORD
        updateStage()

        passwordView.requestFocus()
        // Show the keyboard.
        passwordView.postDelayed(mShowKeyboardRunnable, 500)

        // Fingerprint is not used anymore. Stop listening for it.
        mFingerprintUiHelper?.stopListening()
    }

    /**
     * Checks whether the current entered password is correct, and dismisses the the dialog and
     * let's the activity know about the result.
     */
    private fun verifyPassword() {
        val password = passwordView.text.toString()
        if (!checkPassword(password)) {
            passwordView.error = getString(R.string.authentication_error_incorrect_password)
            return
        }

        if (stage == Stage.NEW_FINGERPRINT_ENROLLED) {
            val editor = mSharedPreferences.edit()
            editor.putBoolean(getString(R.string.authentication_fingerprint_use_to_authenticate_key), useFingerprintInFutureView.isChecked)
            editor.apply()
            if (useFingerprintInFutureView.isChecked) {
                // Re-create the key so that fingerprints including new ones are validated.
                fingerprintInvalidationListener?.invoke(true)
                stage = Stage.FINGERPRINT
            }
        }
        passwordView.setText("")
        authenticationSuccessListener?.invoke()
        dismiss()
    }

    /**
     * @return true if `password` is correct, false otherwise
     */
    private fun checkPassword(password: String): Boolean {
        return passwordVerificationListener?.invoke(password) ?: false
    }

    private val mShowKeyboardRunnable = Runnable { passwordView.showKeyboard() }

    /**
     * Enumeration to indicate which authentication method the user is trying to authenticate with.
     */
    enum class Stage {
        FINGERPRINT,
        NEW_FINGERPRINT_ENROLLED,
        PASSWORD
    }
}