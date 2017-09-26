package co.temy.securitysample

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import java.co.temy.securitysample.encryption.CipherWrapper
import java.co.temy.securitysample.encryption.KeyStoreWrapper
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    companion object {
        val TEST_PASSWORD = "TEST_PASSWORD"
    }

    @Test
    fun testEncryption() {
        val keyStore = KeyStoreWrapper(InstrumentationRegistry.getTargetContext())
        val symmetricKey = keyStore.createAndroidKeyStoreSymmetricKey(TEST_PASSWORD, false, false)

        val cipher = CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC)

        if (symmetricKey != null) {

            val encryptedData = cipher.encrypt(TEST_PASSWORD, symmetricKey)
            val decryptedData = cipher.decrypt(encryptedData, symmetricKey)

            Log.i("EncryptionFragment", "Input Value : $TEST_PASSWORD")
            Log.i("EncryptionFragment", "Encrypted Value : $encryptedData")
            Log.i("EncryptionFragment", "Decrypted Value : $decryptedData")
        }
    }
}
