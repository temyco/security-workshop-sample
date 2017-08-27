package co.temy.securitysample

import android.support.v7.app.AppCompatActivity
import co.temy.securitysample.system.SystemServices

open class BaseSecureActivity : AppCompatActivity() {

    val systemServices by lazy(LazyThreadSafetyMode.NONE) { SystemServices(this) }

    override fun onStart() {
        super.onStart()
        if (!systemServices.isDeviceSecure()) {
            systemServices.showDeviceSecurityAlert()
        }
    }
}