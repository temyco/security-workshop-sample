# Workshop

Link for presentation with all functions descriptions will be placed here.

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

`RSA` - the only one algorithm we can use for Asymmetric Keys till API 23.

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
    val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")

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

Ignore `keyPassword` parameter for now, we will get back to it later on during the workshop. Just run
a project and check the results. 

### Encryption Stage - Level 2

