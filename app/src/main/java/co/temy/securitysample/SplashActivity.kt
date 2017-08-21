package co.temy.securitysample

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.scottyab.safetynet.SafetyNetHelper
import com.scottyab.safetynet.SafetyNetResponse

class SplashActivity : AppCompatActivity() {

    private val API_KEY = BuildConfig.GOOGLE_VERIFICATION_API_KEY
    private val TAG:String = SplashActivity ::class.java.simpleName

    private var progressBar:ProgressBar? = null
    private var statusLabel:TextView? = null

    companion object {
        private val HOME_SCREEN_START_DELAY: Long = 1000
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    private fun checkSafeNet() {
        progressBar = findViewById(R.id.progressBar)
        statusLabel = findViewById(R.id.statusLabel)
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.warning_ttl)
                    .setPositiveButton(R.string.exit_btn, { _, _ -> System.exit(0) })
                    .setNegativeButton(R.string.proceed_btn, { _, _ -> goCheckWithLibrary() })
                    .setMessage(R.string.gms_not_ready_msg)
                    .setCancelable(false)
                    .create()
                    .show()

        } else {
            goCheckWithLibrary ()
        }
    }

    override fun onStart() {
        super.onStart()
        checkSafeNet()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        System.exit(0)
    }

    private fun goCheckWithLibrary () {
        val safetyNetHelper = SafetyNetHelper(API_KEY)
        safetyNetHelper.requestTest(this, object : SafetyNetHelper.SafetyNetWrapperCallback {
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
            Toast.makeText(this, R.string.secure_success_msg, Toast.LENGTH_LONG).show()
            showLoading(false, getString(R.string.secure_success_msg))
            goNextWithDelay ()
        } else if (safetyNetResponse.isBasicIntegrity) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.warning_ttl)
                    .setPositiveButton(R.string.exit_btn, { _, _ -> System.exit(0) })
                    .setNegativeButton(R.string.proceed_btn, { _, _ -> goNextWithDelay() })
                    .setMessage(R.string.insecure_msg)
                    .setCancelable(false)
                    .create()
                    .show()
            showLoading(false, getString(R.string.insecure_msg))

        } else {
            val builder = AlertDialog.Builder(this)
                    .setTitle(R.string.warning_ttl)
                    .setPositiveButton(R.string.exit_btn, { _, _ -> System.exit(0) })
                    .setMessage(R.string.completely_insecure_msg)
                    .setCancelable(false)
            showLoading(false, getString(R.string.completely_insecure_msg))
            if (BuildConfig.DEBUG) {
                builder.setNegativeButton(R.string.proceed_btn, { _, _ -> goNextWithDelay() })
            }
            builder.create().show()
        }
    }

    private fun handleError(errorCode: Int, errorMessage: String) {
        Log.d(TAG, "Something went wrong: " + errorCode)
        showLoading(false, errorMessage)
        AlertDialog.Builder(this)
                .setTitle(R.string.warning_ttl)
                .setPositiveButton(R.string.exit_btn, { _, _ -> System.exit(0) })
                .setNegativeButton(R.string.proceed_btn, { _, _ -> goNextWithDelay() })
                .setMessage(errorMessage)
                .setCancelable(false)
                .create()
                .show()
    }

    private fun showLoading(show: Boolean, message: String) {
        if (show) {
            progressBar?.visibility = View.VISIBLE
            statusLabel?.text = getString(R.string.check_environment_lab)
        } else {
            progressBar?.visibility = View.INVISIBLE
            statusLabel?.text = message
        }
    }

    private fun goNextWithDelay () {
        handler.postDelayed({ startHomeActivity() }, HOME_SCREEN_START_DELAY)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    private fun startHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
