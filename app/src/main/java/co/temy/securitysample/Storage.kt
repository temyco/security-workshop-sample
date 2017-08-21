package co.temy.securitysample

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest

/**
 * Stores application data like password hash.
 * Created by denys on 8/21/17.
 */
class Storage constructor(context: Context) {
    val pref: SharedPreferences

    companion object {
        private val STORAGE_PASSWORD_HASH: String = "pwd_hash"
    }

    init {
        pref = context.getSharedPreferences("storage", android.content.Context.MODE_PRIVATE)
    }

    fun isPasswordSet(): Boolean {
        return pref.contains(STORAGE_PASSWORD_HASH)
    }

    fun setPassword(password: String) {
        val passwordHash = createPasswordHash(password)
        pref.edit().putString(STORAGE_PASSWORD_HASH, passwordHash).apply()
    }

    private fun createPasswordHash(password: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val passwordBytes = password.toByteArray(Charsets.UTF_16)
        val passwordHash = md.digest(passwordBytes)
        val passwordHashString = Base64.encodeToString(passwordHash, Base64.DEFAULT)
        return passwordHashString
    }
}