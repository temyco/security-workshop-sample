package co.temy.securitysample

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    companion object {
        private val HOME_SCREEN_START_DELAY: Long = 1000
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        checkSafeNet();
    }

    private fun checkSafeNet() {

    }

    override fun onPostResume() {
        super.onPostResume()
        handler.postDelayed({ startHomeActivity() }, HOME_SCREEN_START_DELAY)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    fun startHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
