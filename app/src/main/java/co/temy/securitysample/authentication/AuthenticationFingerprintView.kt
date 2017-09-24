package co.temy.securitysample.authentication

import android.content.res.Resources
import android.widget.ImageView
import android.widget.TextView
import co.temy.securitysample.R
import co.temy.securitysample.extentions.getColorCompat

class AuthenticationFingerprintView(private val icon: ImageView, private val errorTextView: TextView) {

    private val resources: Resources = icon.resources

    fun showSuccessView() {
        icon.setImageResource(R.drawable.ic_fingerprint_success)
        errorTextView.setTextColor(resources.getColorCompat(R.color.success_color))
        errorTextView.text = resources.getString(R.string.authentication_fingerprint_success)
    }

    fun showErrorView(errorId: Int) = showErrorView(resources.getString(errorId))

    fun showErrorView(error: String) {
        icon.setImageResource(R.drawable.ic_fingerprint_error)
        errorTextView.text = error
        errorTextView.setTextColor(resources.getColorCompat(R.color.warning_color))
    }

    fun hideErrorView() {
        errorTextView.setTextColor(resources.getColorCompat(R.color.hint_color))
        errorTextView.text = resources.getString(R.string.authentication_fingerprint_hint)
        icon.setImageResource(R.drawable.ic_fp_40px)
    }
}