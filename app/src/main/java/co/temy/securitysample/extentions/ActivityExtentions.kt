package co.temy.securitysample.extentions

import android.app.Activity
import android.content.Intent
import co.temy.securitysample.SecretActivity
import co.temy.securitysample.Storage

fun Activity.startSecretActivity(requestCode: Int, mode: Int = SecretActivity.MODE_CREATE, secretData: Storage.SecretData? = null) {
    val intent = Intent(this, SecretActivity::class.java)
    intent.putExtra("mode", mode)
    secretData.let { intent.putExtra("secret", secretData) }
    startActivityForResult(intent, requestCode)
}