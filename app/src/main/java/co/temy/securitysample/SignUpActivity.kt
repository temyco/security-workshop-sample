package co.temy.securitysample

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.activity_sign_up.*

/**
 * Sign up with password screen.
 */
class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Set up the sing up form.
        passwordView.setOnEditorActionListener({ _, id, _ -> onEditorActionClick(id) })
        doneView.setOnClickListener { attemptToSignUp() }
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
            Storage(this).setPassword(passwordString)
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun isPasswordValid(password: String) = !TextUtils.isEmpty(password) && password.length >= 6
}
