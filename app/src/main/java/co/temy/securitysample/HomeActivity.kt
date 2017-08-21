package co.temy.securitysample

import android.app.KeyguardManager
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnAddPassword: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        btnAddPassword = findViewById(R.id.btnAddPassword)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item -> onNavigationItemSelected(item) }

        showTab(PasswordsFragment())
    }

    override fun onStart() {
        super.onStart()
        checkLockScreen()
    }

    fun onAddPasswordClick(view: View) {
        val intent = Intent(this, AddKeyActivity::class.java)
        startActivity(intent)
    }

    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuPasswords) {
            showTab(PasswordsFragment())
            btnAddPassword.show()
            return true
        } else if (item.itemId == R.id.menuEncryption) {
            showTab(EncryptionFragment())
            btnAddPassword.hide()
            return true
        }
        return false
    }

    private fun showTab(tab: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.tabContainer, tab)
        fragmentTransaction.commitNow()
    }

    private fun checkLockScreen() {
        val kgManager = this.getSystemService(android.content.Context.KEYGUARD_SERVICE) as KeyguardManager

        if(!kgManager.isKeyguardSecure) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.lock_title)
            builder.setMessage(R.string.lock_body)

            builder.setPositiveButton(R.string.lock_settings, { d, i ->
                openSecuritySettings()
            })

            builder.setNegativeButton(R.string.lock_exit, { d, i ->
                finish()
            })

            builder.show()
        } else {
            checkFingerprintSupport()
        }
    }

    private fun checkFingerprintSupport() {
        val fingerPrintManager = this.getSystemService(android.content.Context.FINGERPRINT_SERVICE)
                as FingerprintManager

        val showAlert = fingerPrintManager.isHardwareDetected &&
                        !fingerPrintManager.hasEnrolledFingerprints()

        if (showAlert) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.finger_title)
            builder.setMessage(R.string.finger_body)

            builder.setPositiveButton(R.string.finger_settings, { d, i ->
                openSecuritySettings()
            })

            builder.setNegativeButton(R.string.finger_exit, { d, i ->
                finish()
            })

            builder.show()
        }
    }

    private fun openSecuritySettings() {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        startActivity(intent)
    }
}
