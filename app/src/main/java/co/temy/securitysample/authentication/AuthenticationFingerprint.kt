package co.temy.securitysample.authentication

import android.annotation.TargetApi
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import co.temy.securitysample.R

class AuthenticationFingerprint(
        private val systemServices: SystemServices,
        private val view: AuthenticationFingerprintView,
        private val callback: Callback) {

    companion object {
        private val ERROR_TIMEOUT_MILLIS: Long = 1600
        private val SUCCESS_DELAY_MILLIS: Long = 1300
    }

    private var mCancellationSignal: CancellationSignal? = null
    private var selfCancelled: Boolean = false
    private var handler: Handler = Handler(Looper.getMainLooper())

    fun isFingerprintAuthAvailable(): Boolean {
        return systemServices.isFingerprintHardwareAvailable() && systemServices.hasEnrolledFingerprints()
    }

    fun startListening(cryptoObject: FingerprintManager.CryptoObject) {
        if (isFingerprintAuthAvailable()) {
            mCancellationSignal = CancellationSignal()
            selfCancelled = false
            systemServices.authenticateFingerprint(cryptoObject, mCancellationSignal!!, 0, fingerprintCallback, null)
        }
    }

    fun stopListening() {
        mCancellationSignal?.let {
            it.cancel()
            selfCancelled = true
            mCancellationSignal = null
        }
    }

    private val fingerprintCallback = object : FingerprintManager.AuthenticationCallback() {
        override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
            if (!selfCancelled) {
                view.showErrorView(errString.toString())
                handler.postDelayed({ callback.onAuthenticationError() }, ERROR_TIMEOUT_MILLIS)
            }
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence) {
            view.showErrorView(helpString.toString())
            showErrorAndHideItAfterDelay()
        }

        override fun onAuthenticationFailed() {
            view.showErrorView(R.string.authentication_fingerprint_not_recognized)
            showErrorAndHideItAfterDelay()
        }

        @TargetApi(23)
        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
            handler.removeCallbacks(hideErrorRunnable)
            view.showSuccessView()
            handler.postDelayed({ callback.onAuthenticated(result.cryptoObject) }, SUCCESS_DELAY_MILLIS)
        }

        private fun showErrorAndHideItAfterDelay() {
            handler.removeCallbacks(hideErrorRunnable)
            handler.postDelayed(hideErrorRunnable, ERROR_TIMEOUT_MILLIS)
        }

        private val hideErrorRunnable = Runnable { view.hideErrorView() }
    }

    interface Callback {
        fun onAuthenticated(cryptoObject: FingerprintManager.CryptoObject)
        fun onAuthenticationError()
    }
}

