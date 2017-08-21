package co.temy.securitysample.encryption

import android.annotation.TargetApi
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateException
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.security.auth.x500.X500Principal

class KeyStoreWrapper {

    data class KeyData(val alias: String, val creationDate: Date)

    private val keyStore: KeyStore

    init {
        keyStore = createKeystore()
    }

    /**
     * Returns list of currently existed keys in AndroidKeyStore.
     */
    fun getAllKeyAliases(): List<KeyData> {
        return keyStore.aliases().toList().map { KeyData(it, keyStore.getCreationDate(it)) }
    }

    /**
     * Create symmetric [KeyProperties.KEY_ALGORITHM_AES] key with default [KeyProperties.BLOCK_MODE_CBC] and
     * [KeyProperties.ENCRYPTION_PADDING_PKCS7].
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun createSymmetricKey(alias: String, userAuthenticationRequired: Boolean, invalidatedByBiometricEnrollment: Boolean): SecretKey? {
        val keyGenerator = createKeyGenerator(KeyProperties.KEY_ALGORITHM_AES)

        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                // Require the user to authenticate with a fingerprint to authorize every use
                // of the key
                .setUserAuthenticationRequired(userAuthenticationRequired)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)

        // This is a workaround to avoid crashes on devices whose API level is < 24
        // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
        // visible on API level +24.
        // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
        // which isn't available yet.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)
        }

        try {
            keyGenerator?.init(builder.build())
            return keyGenerator?.generateKey()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Create asymmetric [KeyProperties.KEY_ALGORITHM_AES] key with default [KeyProperties.BLOCK_MODE_CBC] and
     * [KeyProperties.ENCRYPTION_PADDING_PKCS7].
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun createAsymmetricKey(alias: String, userAuthenticationRequired: Boolean, invalidatedByBiometricEnrollment: Boolean): KeyPair? {
        val generator = createKeyPairGenerator(KeyProperties.KEY_ALGORITHM_RSA)

        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance()
        endDate.add(Calendar.YEAR, 20)
        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setCertificateSerialNumber(BigInteger.ONE)
                .setCertificateSubject(X500Principal("CN=${alias} CA Certificate"))
                .setCertificateNotBefore(startDate.time)
                .setCertificateNotAfter(endDate.time)
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)

        try {
            generator?.initialize(builder.build())
            return generator?.generateKeyPair()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun createKeystore(): KeyStore {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            return keyStore
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to get an instance of KeyStore", e)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun createKeyGenerator(algorithm: String): KeyGenerator? {
        try {
            return KeyGenerator.getInstance(algorithm, "AndroidKeyStore")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get an instance of KeyGenerator", e)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException("Failed to get an instance of KeyGenerator", e)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun createKeyPairGenerator(algorithm: String): KeyPairGenerator? {
        try {
            return KeyPairGenerator.getInstance(algorithm, "AndroidKeyStore")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get an instance of KeyGenerator", e)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException("Failed to get an instance of KeyGenerator", e)
        }
    }
}

