package co.temy.securitysample

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.util.*

/**
 * Stores application data like password hash.
 */
class Storage constructor(context: Context) {

    private val settings: SharedPreferences
    private val secrets: SharedPreferences

    data class SecretData(val alias: String, val secret: String)

    companion object {
        private val STORAGE_SETTINGS: String = "settings"
        private val STORAGE_PASSWORD_HASH: String = "password_hash"
        private val STORAGE_SECRETS: String = "secrets"
    }

    init {
        settings = context.getSharedPreferences(STORAGE_SETTINGS, android.content.Context.MODE_PRIVATE)
        secrets = context.getSharedPreferences(STORAGE_SECRETS, android.content.Context.MODE_PRIVATE)
    }

    fun isPasswordSet(): Boolean {
        return settings.contains(STORAGE_PASSWORD_HASH)
    }

    fun setPassword(password: String) {
        val passwordHash = createPasswordHash(password)
        settings.edit().putString(STORAGE_PASSWORD_HASH, passwordHash).apply()
    }

    fun isSecretAliasExists(alias: String): Boolean {
        return secrets.contains(alias)
    }

    fun putSecret(secret: SecretData) {
        secrets.edit().putString(secret.alias, secret.secret).apply()
    }

    fun getSecrets(): List<SecretData> {
        val secretsList = ArrayList<SecretData>()
        val secretsAliases = secrets.all
        secretsAliases.forEach { secretsList.add(SecretData(it.key, it.value.toString())) }
        return secretsList
    }

    private fun createPasswordHash(password: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val passwordBytes = password.toByteArray(Charsets.UTF_16)
        val passwordHash = md.digest(passwordBytes)
        return Base64.encodeToString(passwordHash, Base64.DEFAULT)
    }
}