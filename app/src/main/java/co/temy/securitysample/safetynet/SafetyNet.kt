package co.temy.securitysample.safetynet

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import co.temy.securitysample.BuildConfig
import co.temy.securitysample.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.scottyab.safetynet.SafetyNetHelper
import com.scottyab.safetynet.SafetyNetResponse
import com.scottyab.safetynet.Utils

class SafetyNet(private val context: Context) {

    companion object {
        private val TAG = SafetyNet::class.java.simpleName
    }

    var holder: SafetyNetHolder? = null

    var proceedCallback: (() -> Unit)? = null

    fun printApiKey() = Log.d(TAG, "Android API KEY: ${Utils.getSigningKeyFingerprint(context)}; ${context.packageName}")

    fun checkSafeNet() {
        holder?.onSafetyNetCheckStarted()

        if (!isGooglePlayServicesAvailable()) {
            showWarningAlert(R.string.gms_not_ready_msg) { goCheckWithLibrary() }
        } else goCheckWithLibrary()
    }

    private fun goCheckWithLibrary() {
        val safetyNetHelper = SafetyNetHelper(BuildConfig.GOOGLE_VERIFICATION_API_KEY)
        safetyNetHelper.requestTest(context, object : SafetyNetHelper.SafetyNetWrapperCallback {
            override fun error(errorCode: Int, errorMessage: String) {
                handleError(errorCode, errorMessage)
            }

            override fun success(ctsProfileMatch: Boolean, basicIntegrity: Boolean) {
                Log.d(TAG, "SafetyNet req success: ctsProfileMatch:$ctsProfileMatch and basicIntegrity, $basicIntegrity")
                proceedWithSuccessfulResult(safetyNetHelper.lastResponse)
            }
        })
    }

    private fun proceedWithSuccessfulResult(safetyNetResponse: SafetyNetResponse) {
        if (safetyNetResponse.isBasicIntegrity && safetyNetResponse.isCtsProfileMatch) {
            Toast.makeText(context, R.string.secure_success_msg, Toast.LENGTH_LONG).show()
            holder?.onSafetyNetCheckFinished(context.getString(R.string.secure_success_msg))
            proceedCallback?.invoke()
        } else if (safetyNetResponse.isBasicIntegrity) {
            holder?.onSafetyNetCheckFinished(context.getString(R.string.insecure_msg))
            showWarningAlert(R.string.insecure_msg, proceedCallback)
        } else {
            holder?.onSafetyNetCheckFinished(context.getString(R.string.insecure_msg))
            showWarningAlert(R.string.completely_insecure_msg, if (BuildConfig.DEBUG) proceedCallback else null)
        }
    }

    private fun handleError(errorCode: Int, errorMessage: String) {
        Log.d(TAG, "Something went wrong: $errorCode")
        holder?.onSafetyNetCheckFinished(errorMessage)
        showWarningAlert(errorMessage) { proceedCallback?.invoke() }
    }

    private fun showWarningAlert(messageId: Int, negativeCallback: (() -> Unit)?) = showWarningAlert(context.getString(messageId), negativeCallback)

    private fun showWarningAlert(message: String, negativeCallback: (() -> Unit)?) {
        val builder = AlertDialog.Builder(context)
                .setTitle(R.string.warning_ttl)
                .setPositiveButton(R.string.exit_btn, { _, _ -> System.exit(0) })
                .setMessage(message)
                .setCancelable(false)

        negativeCallback?.let { builder.setNegativeButton(R.string.proceed_btn, { _, _ -> negativeCallback() }) }

        builder.create()
        builder.show()
    }

    private fun isGooglePlayServicesAvailable() = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    interface SafetyNetHolder {
        fun onSafetyNetCheckStarted()
        fun onSafetyNetCheckFinished(message: String)
    }
}
