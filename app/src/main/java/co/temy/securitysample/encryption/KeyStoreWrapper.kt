package co.temy.securitysample.encryption

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import co.temy.securitysample.authentication.SystemServices
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.security.auth.x500.X500Principal

class KeyStoreWrapper(private val context: Context) {

    private val keyStore: KeyStore

    init {
        keyStore = createAndroidKeyStore()
    }

    fun getAndroidKeyStoreSymmetricKey(alias: String): SecretKey = keyStore.getKey(alias, null) as SecretKey

    fun getAndroidKeyStoreAsymmetricKeyPair(alias: String): KeyPair {
        val privateKey = keyStore.getKey(alias, null) as PrivateKey
        val publicKey = keyStore.getCertificate(alias).publicKey
        return KeyPair(publicKey, privateKey)
    }

    /**
     * Generates symmetric [KeyProperties.KEY_ALGORITHM_AES] key with default [KeyProperties.BLOCK_MODE_CBC] and
     * [KeyProperties.ENCRYPTION_PADDING_PKCS7] using default provider.
     */
    fun generateDefaultSymmetricKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        return keyGenerator.generateKey()
    }

    /**
     * Creates symmetric [KeyProperties.KEY_ALGORITHM_AES] key with default [KeyProperties.BLOCK_MODE_CBC] and
     * [KeyProperties.ENCRYPTION_PADDING_PKCS7] and saves it to Android Key Store.
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun createAndroidKeyStoreSymmetricKey(
            alias: String,
            userAuthenticationRequired: Boolean = false,
            invalidatedByBiometricEnrollment: Boolean = true,
            userAuthenticationValidityDurationSeconds: Int = -1,
            userAuthenticationValidWhileOnBody: Boolean = true): SecretKey? {

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                // Require the user to authenticate with a fingerprint to authorize every use of the key
                .setUserAuthenticationRequired(userAuthenticationRequired)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationValidityDurationSeconds(userAuthenticationValidityDurationSeconds)
        // Not working on api 23, try higher ?
        //.setRandomizedEncryptionRequired(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)
            builder.setUserAuthenticationValidWhileOnBody(userAuthenticationValidWhileOnBody)
        }
        keyGenerator?.init(builder.build())
        return keyGenerator?.generateKey()
    }

    /**
     * Creates asymmetric [KeyProperties.KEY_ALGORITHM_AES] key with default [KeyProperties.BLOCK_MODE_CBC] and
     * [KeyProperties.ENCRYPTION_PADDING_PKCS7] and saves it to Android Key Store.
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun createAndroidKeyStoreAsymmetricKey(alias: String): KeyPair {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")

        if (SystemServices.hasMarshmallow()) {
            initGeneratorWithKeyGenParameterSpec(generator, alias)
        } else {
            initGeneratorWithKeyPairGeneratorSpec(generator, alias)
        }

        return generator.generateKeyPair()
    }

    private fun initGeneratorWithKeyPairGeneratorSpec(generator: KeyPairGenerator, alias: String) {
        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance()
        endDate.add(Calendar.YEAR, 20)

        val builder = KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSerialNumber(BigInteger.ONE)
                .setSubject(X500Principal("CN=${alias} CA Certificate"))
                .setStartDate(startDate.time)
                .setEndDate(endDate.time)

        generator.initialize(builder.build())
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun initGeneratorWithKeyGenParameterSpec(generator: KeyPairGenerator, alias: String) {
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
        generator.initialize(builder.build())
    }

    private fun createAndroidKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore
    }
}

