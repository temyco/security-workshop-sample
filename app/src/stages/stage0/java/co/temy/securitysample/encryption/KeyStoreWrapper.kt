package java.co.temy.securitysample.encryption

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

/**
 * This class wraps [KeyStore] class apis with some additional possibilities.
 */
class KeyStoreWrapper(private val context: Context) {

}

