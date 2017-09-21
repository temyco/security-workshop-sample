package co.temy.securitysample

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import java.io.Serializable
import java.util.*

/**
 * Stores application data like password hash.
 */
class Storage constructor(context: Context) {

    private val settings: SharedPreferences
    private val secrets: SharedPreferences

    private val gson: Gson by lazy(LazyThreadSafetyMode.NONE) { Gson() }

    data class SecretData(
            val alias: String,
            val secret: String,
            val createDate: Date,
            val updateDate: Date) : Serializable

    companion object {
        private val STORAGE_SETTINGS: String = "settings"
        private val STORAGE_ENCRYPTION_KEY: String = "encryption_key"
        private val STORAGE_PASSWORD: String = "password"
        private val STORAGE_SECRETS: String = "secrets"
        private val STORAGE_FINGERPRINT: String = "fingerprint_allowed"
    }

    init {
        settings = context.getSharedPreferences(STORAGE_SETTINGS, android.content.Context.MODE_PRIVATE)
        secrets = context.getSharedPreferences(STORAGE_SECRETS, android.content.Context.MODE_PRIVATE)
    }

    fun saveEncryptionKey(key: String) {
        settings.edit().putString(STORAGE_ENCRYPTION_KEY, key).apply()
    }

    fun getEncryptionKey(): String = settings.getString(STORAGE_ENCRYPTION_KEY, "")

    fun isPasswordSaved(): Boolean {
        return settings.contains(STORAGE_PASSWORD)
    }

    fun savePassword(password: String) {
        settings.edit().putString(STORAGE_PASSWORD, password).apply()
    }

    fun getPassword(): String = settings.getString(STORAGE_PASSWORD, "")

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

    fun removeSecret(alias: String) {
        secrets.edit().remove(alias).apply()
    }

    fun getSecrets(): List<SecretData> {
        val secretsList = ArrayList<SecretData>()
        val secretsAliases = secrets.all
        secretsAliases
                .map { gson.fromJson(it.value as String, SecretData::class.java) }
                .forEach { secretsList.add(it) }

        secretsList.sortByDescending { it.createDate }
        return secretsList
    }

    fun clear(){
        settings.edit().clear().apply()
        secrets.edit().clear().apply()
    }
}