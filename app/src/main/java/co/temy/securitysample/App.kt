package co.temy.securitysample

import android.app.Application
import android.util.Log

class App : Application() {
    companion object {
        val TAG = "WORKSHOP"
    }
}

fun logi(message: String) {
    if (BuildConfig.DEBUG) Log.i(App.TAG, message)
}