package co.temy.securitysample

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.view.View
import co.temy.securitysample.BuildConfig.IS_SAFETY_NET_ENABLED
import co.temy.securitysample.extentions.startHomeActivity
import co.temy.securitysample.extentions.startSignUpActivity
import co.temy.securitysample.safetynet.SafetyNet
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : AppCompatActivity(), SafetyNet.SafetyNetHolder {
    companion object {
        private val HOME_SCREEN_START_DELAY: Long = 1000
    }

    private val safetyNet: SafetyNet by lazy(LazyThreadSafetyMode.NONE) {
        val cachedValue = SafetyNet(this)
        cachedValue.holder = this
        cachedValue.proceedCallback = { showNextActivityDelayed() }
        cachedValue
    }

    private val handler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        if (IS_SAFETY_NET_ENABLED) safetyNet.printApiKey()
    }

    override fun onStart() {
        super.onStart()
        if (IS_SAFETY_NET_ENABLED) safetyNet.checkSafeNet() else showNextActivityDelayed()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        System.exit(0)
    }

    override fun onSafetyNetCheckStarted() {
        statusLabelView?.visibility = View.VISIBLE
        progressBarView?.visibility = View.VISIBLE
    }

    override fun onSafetyNetCheckFinished(message: String) {
        progressBarView?.visibility = View.INVISIBLE
        statusLabelView?.text = message
    }

    private fun showNextActivityDelayed() = handler.postDelayed({ showNextActivity() }, HOME_SCREEN_START_DELAY)

    private fun showNextActivity() = if (Storage(this).isPasswordSaved()) startHomeActivity() else startSignUpActivity()
}
