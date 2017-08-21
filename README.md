# Security Sample Application

This application should be build on top of API's 18+, such as AndroidKeyStore, Fingerprint API, Confirm Credentials API.


## Environment setup

We are focusing on the newest tech stuck and environment like Kontlin, to be able to use this application you need to :

- Download and install latest [Android Studio 3.0](https://developer.android.com/studio/preview/index.html)
- Download and install all dependencies that Gradle ask's you to
- Download and install Android Virtual Device(AVD) with API 23, using build in Android Virtual Device Manager from Android Studio
- To cover compatibility issues and support newest features later you will also need to install AVD API 18 and AVD API 24+

## Goals

- Build application based on Android Fingerprint Sample
- Use encryption to create/save keys, encrypt/decrypt data
- Use fingerprint to allow to add/delete keys
- Use confirm credentials to protect key list
- Be as simple as possible

## Workflow & Design

Follow [this link](https://xd.adobe.com/view/25652e67-9814-4633-96fa-1ed8963bcfc0/) to see Sample application Design and try some basic workflow.

## Gists

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
## Resources

### Kotlin

- https://kotlinlang.org/docs/reference/kotlin-doc.html

### Cryptography

- http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html
- https://developer.android.com/training/articles/keystore.html
- https://source.android.com/security/keystore/
- https://github.com/yakivmospan/scytale

### Fingerprint & Credentials API

- https://developer.android.com/about/versions/marshmallow/android-6.0.html#fingerprint-authentication
- https://github.com/googlesamples/android-FingerprintDialog
- https://www.youtube.com/watch?v=VOn7VrTRlA4

### Key Attestation

- https://developer.android.com/training/articles/security-key-attestation.html
- https://developer.android.com/training/safetynet/index.html
- https://github.com/googlesamples/android-key-attestation
- https://github.com/googlesamples/android-play-safetynet/
