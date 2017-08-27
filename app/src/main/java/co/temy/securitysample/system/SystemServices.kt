package co.temy.securitysample.system

import android.annotation.TargetApi
import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.support.v7.app.AlertDialog
import co.temy.securitysample.BuildConfig
import co.temy.securitysample.R
import co.temy.securitysample.extentions.openLockScreenSettings

@TargetApi(Build.VERSION_CODES.M)
class SystemServices(private val context: Context) {

    companion object {
        fun hasMarshmallow() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private val keyguardManager: KeyguardManager
    private var fingerprintManager: FingerprintManager? = null

    init {
        keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (hasMarshmallow()) {
            fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
        }
    }

    fun isDeviceSecure(): Boolean = if (hasMarshmallow()) keyguardManager.isDeviceSecure else keyguardManager.isKeyguardSecure

    fun isFingerprintHardwareAvailable() = fingerprintManager?.isHardwareDetected ?: false

    fun hasEnrolledFingerprints() = fingerprintManager?.hasEnrolledFingerprints() ?: false

    fun showDeviceSecurityAlert(): AlertDialog {
        return AlertDialog.Builder(context)
                .setTitle(R.string.lock_title)
                .setMessage(R.string.lock_body)
                .setPositiveButton(R.string.lock_settings, { d, i -> context.openLockScreenSettings() })
                .setNegativeButton(R.string.lock_exit, { d, i -> System.exit(0) })
                .setCancelable(BuildConfig.DEBUG)
                .show()
    }
}