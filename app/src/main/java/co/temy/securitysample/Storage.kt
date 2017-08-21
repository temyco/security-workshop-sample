package co.temy.securitysample.fingerprint

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest

/**
 * Created by denys on 8/21/17.
 */
class Storage constructor(context: Context) {
    val pref: SharedPreferences

    init {
        pref = context.getSharedPreferences("storage", android.content.Context.MODE_PRIVATE)
    }

    public fun isPasswordSet(): Boolean {
        return pref.contains("pwd_hash")
    }

    public fun setPassword(password: String) {
        val passwordHash = createPasswordHash(password)
        pref.edit().putString("pwd_hash", passwordHash)
    }

    private fun createPasswordHash(password: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val passwordBytes = password.toByteArray(Charsets.UTF_16)
        val passwordHash = md.digest(passwordBytes)
        val passwordHashString = Base64.encodeToString(passwordHash, Base64.DEFAULT)
        return passwordHashString
    }
}