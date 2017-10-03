# Security Code Snippets

- Create `KeyStore`instance and prepare it for working using `AndroidKeyStore` provider:

```kotlin
val keyStore = KeyStore.getInstance("AndroidKeyStore")
keyStore.load(null)
```

- Get information about keys currently existed in keystore:

```kotlin
// Define data class to store key info
data class KeyData(val alias: String, val creationDate: Date)
val keyData = keyStore.aliases().toList().map { KeyData(it, keyStore.getCreationDate(it)) }
```

- Prepare `KeyGenerator` instance for generating and saving `AES` symmetric keys using `AndroidKeyStore` provider:

```kotlin
val generator = KeyGenerator.getInstance(algorithm, "AndroidKeyStore")


val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setUserAuthenticationRequired(userAuthenticationRequired)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)

generator?.init(builder.build())

// This will create and save key to KeyStore
val key = keyGenerator?.generateKey()
```

- Prepare `KeyPairGenerator` instance for generating and saving `RSA` asymmetric key pairs using `AndroidKeyStore` provider:

```kotlin
val generator = KeyPairGenerator.getInstance(keyProps.mKeyType, provider);

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

generator.initialize(generator.build())

// This will create and save key to KeyStore
val keyPair = generator?.generateKeyPair()
```