package co.temy.securitysample

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.gson.Gson
import java.security.MessageDigest
import java.util.*

/**
 * Stores application data like password hash.
 */
class Storage constructor(context: Context) {

    private val settings: SharedPreferences
    private val secrets: SharedPreferences

    private val gson: Gson by lazy(LazyThreadSafetyMode.NONE) { Gson() }

    data class SecretData(val alias: String, val secret: String, val date: Date)

    companion object {
        private val STORAGE_SETTINGS: String = "settings"
        private val STORAGE_PASSWORD_HASH: String = "password_hash"
        private val STORAGE_SECRETS: String = "secrets"
        private val STORAGE_FINGERPRINT: String = "fingerprint_allowed"
    }

    init {
        settings = context.getSharedPreferences(STORAGE_SETTINGS, android.content.Context.MODE_PRIVATE)
        secrets = context.getSharedPreferences(STORAGE_SECRETS, android.content.Context.MODE_PRIVATE)
    }

    fun isPasswordSaved(): Boolean {
        return settings.contains(STORAGE_PASSWORD_HASH)
    }

    fun savePassword(password: String) {
        val passwordHash = createPasswordHash(password)
        settings.edit().putString(STORAGE_PASSWORD_HASH, passwordHash).apply()
    }

    fun saveFingerprintAllowed(allowed: Boolean) {
        settings.edit().putBoolean(STORAGE_FINGERPRINT, allowed).apply()
    }

    fun isFingerprintAllowed(): Boolean {
        return settings.getBoolean(STORAGE_FINGERPRINT, false)
    }

    fun hasSecret(alias: String): Boolean {
        return secrets.contains(alias)
    }

    fun saveSecret(secret: SecretData) {
        secrets.edit().putString(secret.alias, gson.toJson(secret)).apply()
    }

    fun getSecrets(): List<SecretData> {
        val secretsList = ArrayList<SecretData>()
        val secretsAliases = secrets.all
        secretsAliases.map { gson.fromJson(it.value as String, SecretData::class.java) }.forEach { secretsList.add(it) }
        return secretsList
    }

    private fun createPasswordHash(password: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val passwordBytes = password.toByteArray(Charsets.UTF_16)
        val passwordHash = md.digest(passwordBytes)
        return Base64.encodeToString(passwordHash, Base64.DEFAULT)
    }
}