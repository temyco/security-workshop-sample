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
    }

    override fun onPostResume() {
        super.onPostResume()
        handler.postDelayed( {
                    if(Storage(this).isPasswordSet()) {
                        startHomeActivity()
                    } else {
                        startSignUpActivity();
                    }

                }, HOME_SCREEN_START_DELAY)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    private fun startHomeActivity() {
        startActivity(HomeActivity::class.java)
    }

    private fun startSignUpActivity() {
        startActivity(SignUpActivity::class.java)
    }

    private fun startActivity(cls:Class<*>) {
        val intent = Intent(this, cls)
        startActivity(intent)
        finish()
    }
}
