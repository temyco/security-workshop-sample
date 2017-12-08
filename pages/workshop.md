# Workshop

Check out the [presentation link](https://speakerdeck.com/yakivmospan/secure-data-in-android) and [article series](https://proandroiddev.com/secure-data-in-android-encryption-7eda33e68f58) for even more interesting details.

## Project Structure

Project is separated on different Stages using gradle flavors. Stage represents some task, that need to be completed.
Stage can have subtasks - levels.

![](/assets/workshop-1.png)

Encryption Stage

![](/assets/workshop-2.png)

Fingerprint Stage

![](/assets/workshop-3.png)

Confirm Credentials Stage

![](/assets/workshop-4.png)

Origination Stage. We are beginning from it and will update it during the workshop.

![](/assets/workshop-5.png)

Completed Workshop Flavor

![](/assets/workshop-6.png)

Classes that we will update during the workshop

![](/assets/workshop-7.png)

If you have lost focus or something just went wrong - select next Stage or Level and continue to work on it.

Note, it is important to reset application data before moving to another stage. You can do this from in application menu or device settings.

![](/assets/workshop-8.png)

## Workshop Guide

### Encryption Stage - Level 1

#### Keyguard

Our first requirement is to make sure that user Lock Screen is setup. The implementation of this is pretty easy.
Please open `SystemServices` class. It is a wrapper of `Context` services that we will use in this project.
And one of those service is `KeyguardManager` service. We will use it to check if device / keyguard is secure.
It is already implemented, lets just check how it works:

```kotlin
// Initialize keyguard service
private val keyguardManager: KeyguardManager

init {
    keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
}

// Compatibility handling, keyguardManager.isDeviceSecure available only from Android M
fun isDeviceSecure(): Boolean = if (hasMarshmallow()) keyguardManager.isDeviceSecure else keyguardManager.isKeyguardSecure

// Show alert dialog on start of each screen, to prevent using our app with out Lock Screen
fun showDeviceSecurityAlert(): AlertDialog {
    return AlertDialog.Builder(context)
            .setTitle(R.string.lock_title)
            .setMessage(R.string.lock_body)
            .setPositiveButton(R.string.lock_settings, { _, _ -> context.openLockScreenSettings() })
            .setNegativeButton(R.string.lock_exit, { _, _ -> System.exit(0) })
            .setCancelable(BuildConfig.DEBUG)
            .show()
}
```

#### Select a Key

Next step is to protect user sensitive data, like password and Secrets. We already know that encryption will be used for this.
And first what we need to do is to choose what Key (symmetric, asymmetric) and Algorithm to use.

Also we know that Symmetric Keys are available from Android 23+ API, and Asymmetric from 18+ API. Our choice is predictable,
we will use Asymmetric Keys, but still, what algorithm to chose? Lets search for help in documentation :

![](/assets/workshop-9.png)

`RSA` - the only one algorithm we can use for Asymmetric Keys.

#### KeyStoreWrapper

Now, when we know the Key and Algorithm, lets create a storage for it. Make sure you are on `stage0` flavor and open up
a `KeyStoreWrapper`. Here we will put all code related to Keys  generation, storage, retrieving and removing. Lets begin.

Add `keyStore` field:

```kotlin
private val keyStore: KeyStore = createAndroidKeyStore()
```

Add `createAndroidKeyStore` function, that creates a keystore instance for `AndroidKeyStore` provider and loads keys from system:

```kotlin
private fun createAndroidKeyStore(): KeyStore {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)
    return keyStore
}
```

Add `createAndroidKeyStoreAsymmetricKey` function, that creates `RSA` public - private key pair with two different methods,
depending or running Android version:

```kotlin
fun createAndroidKeyStoreAsymmetricKey(alias: String): KeyPair {
    val generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")

    if (SystemServices.hasMarshmallow()) {
        initGeneratorWithKeyGenParameterSpec(generator, alias)
    } else {
        initGeneratorWithKeyPairGeneratorSpec(generator, alias)
    }

    return generator.generateKeyPair()
}
```

Add `initGeneratorWithKeyGenParameterSpec` function, that users `KeyGenParameterSpec` to specify key details, and is available
only from API 23+:

```kotlin
@TargetApi(Build.VERSION_CODES.M)
private fun initGeneratorWithKeyGenParameterSpec(generator: KeyPairGenerator, alias: String) {
    val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
    generator.initialize(builder.build())
}
```

Add `initGeneratorWithKeyPairGeneratorSpec` function,  that users `KeyPairGeneratorSpec` to specify key details, and is available
from API 18+, but is deprecated in API 23:

```kotlin
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
```

Add `getAndroidKeyStoreAsymmetricKeyPair` function, that gets created key from Android Key Store or returns `null` if there no
key with given alias:

```kotlin
fun getAndroidKeyStoreAsymmetricKeyPair(alias: String): KeyPair? {
       val privateKey = keyStore.getKey(alias, null) as PrivateKey?
       val publicKey = keyStore.getCertificate(alias)?.publicKey

   return if (privateKey != null && publicKey != null) {
       KeyPair(publicKey, privateKey)
   } else {
       null
   }
}
```

Add `removeAndroidKeyStoreKey` function, that removes key with given alias from Android Key Store:

```kotlin
fun removeAndroidKeyStoreKey(alias: String) = keyStore.deleteEntry(alias)
```

#### CipherWrapper

Great, we can create, store, retrieve and remove Android Key Store Asymmetric Keys. Lets use them now. Please open `CipherWrapper`
class. Here we will put all code related to encryption and decryption.

Add `cipher` field:

```kotlin
val cipher: Cipher = Cipher.getInstance(transformation)
```

Add `transformation` parameter to `CipherWrapper` default constructor:

```kotlin
class CipherWrapper(val transformation: String) {
}
```

Add `TRANSFORMATION_ASYMMETRIC` constant, that represents a schema to encrypt / decrypt with for asymmetric keys:

```kotlin
companion object {
    var TRANSFORMATION_ASYMMETRIC = "RSA/ECB/PKCS1Padding"
}
```

Add `encrypt` method, that initializes cipher to encryption mode with given key and encrypts provided data:

```kotlin
fun encrypt(data: String, key: Key?): String {
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val bytes = cipher.doFinal(data.toByteArray())
    return Base64.encodeToString(bytes, Base64.DEFAULT)
}
```

Add `decrypt` method, that initializes cipher to decryption mode with given key and decrypts provided data:

```kotlin
fun decrypt(data: String, key: Key?): String {
    cipher.init(Cipher.DECRYPT_MODE, key)
    val encryptedData = Base64.decode(data, Base64.DEFAULT)
    val decodedData = cipher.doFinal(encryptedData)
    return String(decodedData)
}
```

#### EncryptionServices

Now, having all puzzle part, lets put them together. Lets create user Master Key and encrypt, decrypt password and Secrets
with it.

Please open`EncryptionServices` class. It is a main interface of predefined functions that we are going to modify
during the workshop.

Add `keyStoreWrapper` field:

```kotlin
private val keyStoreWrapper = KeyStoreWrapper(context)
```

Add `MASTER_KEY` alias constant:

```kotlin
companion object {
    val MASTER_KEY = "MASTER_KEY"
}
```

Update `createMasterKey` function:

```kotlin
fun createMasterKey(keyPassword: String? = null) {
    keyStoreWrapper.createAndroidKeyStoreAsymmetricKey(MASTER_KEY)
}
```

Update `removeMasterKey` function:

```kotlin
fun removeMasterKey() {
    keyStoreWrapper.removeAndroidKeyStoreKey(MASTER_KEY)
}
```

Update `encrypt` function:

```kotlin
fun encrypt(data: String, keyPassword: String? = null): String {
    val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(MASTER_KEY)
    return CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).encrypt(data, masterKey?.public)
}
```

Update `decrypt` function:

```kotlin
fun decrypt(data: String, keyPassword: String? = null): String {
    val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(MASTER_KEY)
    return CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).decrypt(data, masterKey?.private)
}
```

Ignore `keyPassword` parameter for now, we will get back to it later on. Just run a project and check the results.

### Encryption Stage - Level 2

Before we continue, please try to save Large Secret (more then 250 symbols). Oops, 'IllegalBlockSizeException'. Unfortunately
`RSA` keys was desired to work with small amount of data. Message length depends on the key size, the bigger key is,
the bigger message can be encrypted. Be aware that using big key size will increase encryption time and may affect
application performance.

Our great plan was ruined. And now we are in bad situation, we cannot use asymmetric key, and symmetric is available only
from API 23+. To escape it we can choose on of two paths:

1. Create symmetric key with default Java Provider. Encrypt / decrypt password and Secrets with it. Encrypt this key raw data
with our `RSA` public key and save it somewhere to the disk. Then when we need to decrypt something, get encrypted key data,
decrypt it with `RSA` private key and use it for data decryption.

2. Separate large message on parts and encrypt / decrypt each of the part individually.

Second option looks easier in implementation, but again, `RSA` is not desired for tasks like this. It will be more
secure to continue with first option.

>_Please, before we continue, reset application data from in app menu or in device settings._

#### KeyStoreWrapper

And we will start from default provider symmetric key generation. Open `KeyStoreWrapper` class and add `generateDefaultSymmetricKey`
function, that creates symmetric `AES` key instance for default Java Provider:

```kotlin
fun generateDefaultSymmetricKey(): SecretKey {
    val keyGenerator = KeyGenerator.getInstance("AES")
    return keyGenerator.generateKey()
}
```

Add `createAndroidKeyStoreSymmetricKey`, that creates symmetric `AES` key instance for `AndroidKeyStore` and will be used
in Android API 23+:

```kotlin
@TargetApi(23)
fun createAndroidKeyStoreSymmetricKey(alias: String): SecretKey {
    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
    keyGenerator.init(builder.build())
    return keyGenerator.generateKey()
}
```

Add `getAndroidKeyStoreSymmetricKey` function:

```kotlin
fun getAndroidKeyStoreSymmetricKey(alias: String): SecretKey? = keyStore.getKey(alias, null) as SecretKey?
```

#### CipherWrapper

Now, lets protect a key. Please open `CipherWrapper` class and add `wrapKey` function, that encrypts one key with another:

```kotlin
fun wrapKey(keyToBeWrapped: Key, keyToWrapWith: Key?): String {
    cipher.init(Cipher.WRAP_MODE, keyToWrapWith)
    val decodedData = cipher.wrap(keyToBeWrapped)
    return Base64.encodeToString(decodedData, Base64.DEFAULT)
}
```

Add `unWrapKey` function, that decrypts a key using another one:

```kotlin
fun unWrapKey(wrappedKeyData: String, algorithm: String, wrappedKeyType: Int, keyToUnWrapWith: Key?): Key {
    val encryptedKeyData = Base64.decode(wrappedKeyData, Base64.DEFAULT)
    cipher.init(Cipher.UNWRAP_MODE, keyToUnWrapWith)
    return cipher.unwrap(encryptedKeyData, algorithm, wrappedKeyType)
}
```

Add `TRANSFORMATION_SYMMETRIC` constant, that will be used for our symmetric keys:

```kotlin
companion object {
    var TRANSFORMATION_SYMMETRIC = "AES/CBC/PKCS7Padding"
}
```

#### EncryptionServices

Add `storage` field, that is a simple `SharedPreferences` wrapper. We will store encrypted key there:

```kotlin
private val storage = Storage(context)
```

Update `createMasterKey` function:

```kotlin
fun createMasterKey(keyPassword: String? = null) {
    if (SystemServices.hasMarshmallow()) {
        createAndroidSymmetricKey()
    } else {
        createDefaultSymmetricKey()
    }
}
```

Add `createAndroidSymmetricKey` function:

```kotlin
private fun createAndroidSymmetricKey() {
    keyStoreWrapper.createAndroidKeyStoreSymmetricKey(MASTER_KEY)
}
```

Add `createDefaultSymmetricKey` function:

```kotlin
private fun createDefaultSymmetricKey() {
    val symmetricKey = keyStoreWrapper.generateDefaultSymmetricKey()
    val masterKey = keyStoreWrapper.createAndroidKeyStoreAsymmetricKey(MASTER_KEY)
    val encryptedSymmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).wrapKey(symmetricKey, masterKey.public)
    storage.saveEncryptionKey(encryptedSymmetricKey)
}
```

Update `encrypt` and `decrypt` functions:

```kotlin
fun encrypt(data: String, keyPassword: String? = null): String {
    return if (SystemServices.hasMarshmallow()) {
        encryptWithAndroidSymmetricKey(data)
    } else {
        encryptWithDefaultSymmetricKey(data)
    }
}

fun decrypt(data: String, keyPassword: String? = null): String {
    return if (SystemServices.hasMarshmallow()) {
        decryptWithAndroidSymmetricKey(data)
    } else {
        decryptWithDefaultSymmetricKey(data)
    }
}

private fun encryptWithAndroidSymmetricKey(data: String): String {
    val masterKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY)
    return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, masterKey)
}

private fun decryptWithAndroidSymmetricKey(data: String): String {
    val masterKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY)
    return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, masterKey)
}

private fun encryptWithDefaultSymmetricKey(data: String): String {
    val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(MASTER_KEY)
    val encryptionKey = storage.getEncryptionKey()
    val symmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).unWrapKey(encryptionKey, ALGORITHM_AES, Cipher.SECRET_KEY, masterKey?.private) as SecretKey
    return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, symmetricKey)
}

private fun decryptWithDefaultSymmetricKey(data: String): String {
    val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(MASTER_KEY)
    val encryptionKey = storage.getEncryptionKey()
    val symmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).unWrapKey(encryptionKey, ALGORITHM_AES, Cipher.SECRET_KEY, masterKey?.private) as SecretKey
    return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, symmetricKey)
}
```

### Encryption Stage - Level 3

Lets check the results before we move on. Please run our sample. And another Oops here, `InvalidKeyException:
IV required when decrypting. Use IvParameterSpec or AlgorithmParameters to provide it`.

Initialization Vector is a fixed-size input to a cryptographic primitive. It is typically required to be random or pseudorandom.
The point of an IV is to tolerate the use of the same key to encrypt several distinct messages.

And it is required to be used with block algorithm modes, like `CBC` in `AES` algorithm. Lets implement it.

>_Please, before we continue, reset application data from in app menu or in device settings._

#### CipherWrapper

Open `CipherWrapper` class and update `encrypt` function, that gets system automatically generated Initialization Vector
and adds it to the encryption result as a prefix:

```kotlin
fun encrypt(data: String, key: Key?, useInitializationVector: Boolean = false): String {
    cipher.init(Cipher.ENCRYPT_MODE, key)

    var result = ""
    if (useInitializationVector) {
        val iv = cipher.iv
        val ivString = Base64.encodeToString(iv, Base64.DEFAULT)
        result = ivString + IV_SEPARATOR
    }
    val bytes = cipher.doFinal(data.toByteArray())
    result += Base64.encodeToString(bytes, Base64.DEFAULT)

    return result
}
```

Update `decrypt` function, that parses result text from encrypt method and uses IV in decryption:

```kotlin
fun decrypt(data: String, key: Key?, useInitializationVector: Boolean = false): String {
    var encodedString: String

    if (useInitializationVector) {
        val split = data.split(IV_SEPARATOR.toRegex())
        if (split.size != 2) throw IllegalArgumentException("Passed data is incorrect. There was no IV specified with it.")

        val ivString = split[0]
        encodedString = split[1]
        val ivSpec = IvParameterSpec(Base64.decode(ivString, Base64.DEFAULT))
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
    } else {
        encodedString = data
        cipher.init(Cipher.DECRYPT_MODE, key)
    }

    val encryptedData = Base64.decode(encodedString, Base64.DEFAULT)
    val decodedData = cipher.doFinal(encryptedData)
    return String(decodedData)
}
```

#### EncryptionServices

Open `EncryptionServices` class and update all functions where symmetric key is used.

Update `encryptWithAndroidSymmetricKey` and `decryptWithAndroidSymmetricKey` functions:

```kotlin
private fun encryptWithAndroidSymmetricKey(data: String): String {
    val masterKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY)
    return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, masterKey, true)
}

private fun decryptWithAndroidSymmetricKey(data: String): String {
    val masterKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY)
    return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, masterKey, true)
}
```

Update `encryptWithDefaultSymmetricKey` and `decryptWithDefaultSymmetricKey` functions:

```kotlin
private fun encryptWithDefaultSymmetricKey(data: String): String {
    val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(MASTER_KEY)
    val encryptionKey = storage.getEncryptionKey()
    val symmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).unWrapKey(encryptionKey, ALGORITHM_AES, Cipher.SECRET_KEY, masterKey?.private) as SecretKey
    return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, symmetricKey, true)
}

private fun decryptWithDefaultSymmetricKey(data: String): String {
    val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(MASTER_KEY)
    val encryptionKey = storage.getEncryptionKey()
    val symmetricKey = CipherWrapper(CipherWrapper.TRANSFORMATION_ASYMMETRIC).unWrapKey(encryptionKey, ALGORITHM_AES, Cipher.SECRET_KEY, masterKey?.private) as SecretKey
    return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, symmetricKey, true)
}
```

Run the results, now everything should be ok.

### Encryption Stage - Level 4

There is a nice `KeyPairGeneratorSpec.setEncryptionRequired()` method, that :

>This will protect the key pair with the secure lock screen credential (e.g., password, PIN, or pattern). 
>
>Note that this feature requires that the secure lock screen (e.g., password, PIN, pattern) is set up, otherwise
>key pair generation will fail. Moreover, this key pair will be deleted when the secure lock screen is disabled or
>reset (e.g., by the user or a Device Administrator).

And there is one issue with it, that is very simple to reproduce, on pre API 23, keys will be removed even if `setEncryptionRequired`
is not set. 

Just try to change Lock Screen type and all of your AndroidKeyStore keys will gone.

What to do ?

- Android Key Store may be used safely on M devices and later
- Before M, reload data when keys are invalidated
- Do not use Android Key Store for local only content 
- Instead prefer to use default java Provider (or other)

>_Please, before we continue, reset application data from in app menu or in device settings._

#### KeyStoreWrapper

Add `defaultKeyStoreName` parameter to constructor:

```kotlin
class KeyStoreWrapper(private val context: Context, defaultKeyStoreName: String)
```

Add `defaultKeyStoreFile` field, that points to default keystore location on the disk:

```kotlin
private val defaultKeyStoreFile = File(context.filesDir, defaultKeyStoreName)
```

Add `defaultKeyStore` field and `createDefaultKeyStore` function, that initializes new default provider keystore or loads it from file:

```kotlin
private val defaultKeyStore = createDefaultKeyStore()

private fun createDefaultKeyStore(): KeyStore {
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

    if (!defaultKeyStoreFile.exists()) {
        keyStore.load(null)
    } else {
        keyStore.load(FileInputStream(defaultKeyStoreFile), null)
    }
    return keyStore
}
```

Add `createDefaultKeyStoreSymmetricKey` function, that generates symmetric key and stores it in keystore with given alias and
password:

```kotlin
fun createDefaultKeyStoreSymmetricKey(alias: String, password: String) {
    val key = generateDefaultSymmetricKey()
    val keyEntry = KeyStore.SecretKeyEntry(key)

    defaultKeyStore.setEntry(alias, keyEntry, KeyStore.PasswordProtection(password.toCharArray()))
    defaultKeyStore.store(FileOutputStream(defaultKeyStoreFile), password.toCharArray())
}
```

Add `getDefaultKeyStoreSymmetricKey` function, that gets Symmetric key from default keystore:

```kotlin
fun getDefaultKeyStoreSymmetricKey(alias: String, keyPassword: String): SecretKey? {
    return try {
        defaultKeyStore.getKey(alias, keyPassword.toCharArray()) as SecretKey
    } catch (e: UnrecoverableKeyException) {
        null
    }
}
```

#### EncryptionServices

Open `EncryptionServices` and apply our changes so that only one symmetric key from default keystore will be used, instead
off wrapping symmetric key with RSA key from Android Key Store.

Update `createDefaultSymmetricKey` function:

```kotlin
private fun createDefaultSymmetricKey(password: String) {
    keyStoreWrapper.createDefaultKeyStoreSymmetricKey(MASTER_KEY, password)
}
```

Update `encryptWithDefaultSymmetricKey` and `decryptWithDefaultSymmetricKey` functions:

```kotlin
private fun encryptWithDefaultSymmetricKey(data: String, keyPassword: String): String {
    val masterKey = keyStoreWrapper.getDefaultKeyStoreSymmetricKey(MASTER_KEY, keyPassword)
    return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, masterKey, true)
}

private fun decryptWithDefaultSymmetricKey(data: String, keyPassword: String): String {
    val masterKey = keyStoreWrapper.getDefaultKeyStoreSymmetricKey(MASTER_KEY, keyPassword)
    return masterKey?.let { CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, masterKey, true) } ?: ""
}
```

Run application on both 18 and 23 AVDs and validate that everything is working as desired.

### Fingerprint Stage

In most cases fingerprint is used as optional authentication. It is tied to `AndroidKeyStore` and requires to create
Fingerprint cryptographic Key. Those keys gets invalidated when new fingerprint is added or any one of existed is removed.

There is issue with emulator AVD 24 API - fingerprint key doesn't get invalidated when new fingerprints are enrolled (or
old removed). This also is valid for real devices, Samsung S6 running on API 24 has the same issue.

There is another system service responsible for fingerprint management called `FingerprintManager`, available
from API 23. Please open `SystemServices` class and lets see how it works.

```kotlin
/**
 * There is a nice [FingerprintManagerCompat] class that makes all dirty work for us, but as always, shit happens.
 * Behind the scenes it is using `Context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)`
 * method, that is returning false on 23 API emulators, when in fact [FingerprintManager] is there and is working fine.
 */
private var fingerprintManager: FingerprintManager? = null

init {
    if (hasMarshmallow()) {
        fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
    }
}

// Check if fingerprint hardware is available on device
fun isFingerprintHardwareAvailable() = fingerprintManager?.isHardwareDetected ?: false

// Check if there are fingerprints added
fun hasEnrolledFingerprints() = fingerprintManager?.hasEnrolledFingerprints() ?: false

// Warm up the fingerprint hardware and starts scanning for a fingerprint
fun authenticateFingerprint(cryptoObject: FingerprintManager.CryptoObject, cancellationSignal: CancellationSignal, flags: Int, callback: FingerprintManager.AuthenticationCallback, handler: Handler?) {
    fingerprintManager?.authenticate(cryptoObject, cancellationSignal, flags, callback, handler)
}
```

`FingerprintManager.AuthenticationCallback` is already implemented in `AuthenticationFingerprint` class, please open it.
This class is updating user with authentication results and in case of success, passing us back initialized `CryptoObject`.
Lets check it out:

```kotlin
fun startListening(cryptoObject: FingerprintManager.CryptoObject) {
    // Start fingerprint authentication
}

fun stopListening() {
    // Cancel fingerprint authentication
}

private val fingerprintCallback = object : FingerprintManager.AuthenticationCallback() {
    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
        // To many tries was made, show error text and change view, so user will be able to enter his password
    }

    override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence) {
        // Fingerprint was not recognized, show error with help text and let him try again after some delay
    }

    override fun onAuthenticationFailed() {
        // Fingerprint was not recognized, show error to user and let him try again after some delay
    }

    @TargetApi(23)
    override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
        // Update user with success result and initialized CryptoObject
    }
}
```

>_Please, before we continue, reset application data from in app menu or in device settings._

#### KeyStoreWrapper

First of all we need to create fingerprint cryptographic key. Open `KeyStoreWrapper` class and update `createAndroidKeyStoreSymmetricKey`
function:

```kotlin
@TargetApi(Build.VERSION_CODES.M)
fun createAndroidKeyStoreSymmetricKey(
        alias: String,
        userAuthenticationRequired: Boolean = false,
        invalidatedByBiometricEnrollment: Boolean = true): SecretKey {

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setUserAuthenticationRequired(userAuthenticationRequired)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)
    }
    keyGenerator.init(builder.build())
    return keyGenerator.generateKey()
}
```

#### EncryptionServices

Then open `EncryptionServices` class and:

Update `createFingerprintKey` and `removeFingerprintKey` functions:

```kotlin
fun createFingerprintKey() {
    if (SystemServices.hasMarshmallow()) {
        keyStoreWrapper.createAndroidKeyStoreSymmetricKey(FINGERPRINT_KEY,
                        userAuthenticationRequired = true,
                        invalidatedByBiometricEnrollment = true,
                        userAuthenticationValidWhileOnBody = false)
    }
}

fun removeFingerprintKey() {
    if (SystemServices.hasMarshmallow()) {
        keyStoreWrapper.removeAndroidKeyStoreKey(FINGERPRINT_KEY)
    }
}
```

Update `prepareFingerprintCryptoObject` function:

```kotlin
fun prepareFingerprintCryptoObject(): FingerprintManager.CryptoObject? {
    return if (SystemServices.hasMarshmallow()) {
        try {
            val symmetricKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(FINGERPRINT_KEY)
            val cipher = CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).cipher
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)
            FingerprintManager.CryptoObject(cipher)
        } catch (e: Throwable) {
            // VerifyError is will be thrown on API lower then 23 if we will use unedited
            // class reference directly in catch block
            if (e is KeyPermanentlyInvalidatedException || e is IllegalBlockSizeException) {
                return null
            } else if (e is InvalidKeyException) {
                // Fingerprint key was not generated
                return null
            }
            throw e
        }
    } else null
}
```

Update `validateFingerprintAuthentication` function:

```kotlin
@TargetApi(23)
fun validateFingerprintAuthentication(cryptoObject: FingerprintManager.CryptoObject): Boolean {
    try {
        cryptoObject.cipher.doFinal(KEY_VALIDATION_DATA)
        return true
    } catch (e: Throwable) {
        if (e is KeyPermanentlyInvalidatedException || e is IllegalBlockSizeException) {
            return false
        }
        throw e
    }
}
```

Run the results and validate that everything is working well.

### Confirm Credentials Stage

The last step of our Workshop is to ask user for Lock Screen password for Application authentication. Confirm Credentials
API will help us with this. Like Fingerprint API it is also was added in Android M and is also connected to AndroidKeyStore.

If you thought that Lock Screen equals to `KeyguardManager`, you were absolutely correct. Please open `SystemServices` class 
and take a look on `showAuthenticationScreen` function implementation:

```kotlin
fun showAuthenticationScreen(activity: Activity, requestCode: Int, title: String? = null, description: String? = null) {
    if (hasMarshmallow()) {
        // Creates intent for launching the activity or null if no password is required(no Lock Screen setup).
        // It is available from API 21+ and can be used without cryptographic keys (but it will be not possible to
        // specify user authentication validity duration seconds without it)
        val intent = keyguardManager.createConfirmDeviceCredentialIntent(title, description)
        
        // Start Lock Screen activity with confirm credentials intent and wait for RESULT_OK
        if (intent != null) {
            activity.startActivityForResult(intent, requestCode)
        }
    }
}
```

Now lets create a crypto key that will be authenticated with this intent and implement the authentication validation for it.

>_Please, before we continue, reset application data from in app menu or in device settings._

#### KeyStoreWrapper

Update `createAndroidKeyStoreSymmetricKey` function, that now will allow us to create key for Confirm Credentials:

```kotlin
@TargetApi(Build.VERSION_CODES.M)
fun createAndroidKeyStoreSymmetricKey(
        alias: String,
        userAuthenticationRequired: Boolean = false,
        invalidatedByBiometricEnrollment: Boolean = true,
        userAuthenticationValidityDurationSeconds: Int = -1,
        userAuthenticationValidWhileOnBody: Boolean = true): SecretKey {

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setUserAuthenticationRequired(userAuthenticationRequired)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationValidityDurationSeconds(userAuthenticationValidityDurationSeconds)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)
        builder.setUserAuthenticationValidWhileOnBody(userAuthenticationValidWhileOnBody)
    }
    keyGenerator.init(builder.build())
    return keyGenerator.generateKey()
}
```

#### EncryptionServices

Update `createConfirmCredentialsKey` function:

```kotlin
fun createConfirmCredentialsKey() {
    if (SystemServices.hasMarshmallow()) {
        keyStoreWrapper.createAndroidKeyStoreSymmetricKey(
                CONFIRM_CREDENTIALS_KEY,
                userAuthenticationRequired = true,
                userAuthenticationValidityDurationSeconds = CONFIRM_CREDENTIALS_VALIDATION_DELAY)
    }
}
```

Update `removeConfirmCredentialsKey` function:

```kotlin
fun removeConfirmCredentialsKey() {
    keyStoreWrapper.removeAndroidKeyStoreKey(CONFIRM_CREDENTIALS_KEY)
}
```

Update `validateConfirmCredentialsAuthentication` function:

```kotlin
fun validateConfirmCredentialsAuthentication(): Boolean {
    if (!SystemServices.hasMarshmallow()) {
        return true
    }

    val symmetricKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(CONFIRM_CREDENTIALS_KEY)
    val cipherWrapper = CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC)

    try {
        return if (symmetricKey != null) {
            cipherWrapper.encrypt(KEY_VALIDATION_DATA.toString(), symmetricKey)
            true
        } else false
    } catch (e: Throwable) {
        // VerifyError is will be thrown on API lower then 23 if we will use unedited
        // class reference directly in catch block
        if (e is UserNotAuthenticatedException || e is KeyPermanentlyInvalidatedException) {
            // User is not authenticated or the lock screen has been disabled or reset
            return false
        } else if (e is InvalidKeyException) {
            // Confirm Credentials key was not generated
            return false
        }
        throw e
    }
}
```
That's it, now we can validate key with user Lock Screen password. It's time for testing, please run application on
AVD 23 and validate that everything is working as expected.

Thanks for going through this workshop. Hope you had some fun and learned something interesting during the session. Keep your data secured! 
