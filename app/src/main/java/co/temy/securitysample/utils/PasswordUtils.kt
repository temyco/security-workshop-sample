package co.temy.securitysample.utils

import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Base64

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException

class PasswordUtils {

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun encryptPassword(cipher: Cipher, password: String) {
        try {
            val bytes = cipher.doFinal(password.toByteArray())
            val encryptedPassword = Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (exception: IllegalBlockSizeException) {
            throw RuntimeException("Failed to encrypt password", exception)
        } catch (exception: BadPaddingException) {
            throw RuntimeException("Failed to encrypt password", exception)
        }

    }

    private fun decryptPassword(cipher: Cipher, password: String): String {
        try {
            val bytes = Base64.decode(password, Base64.NO_WRAP)
            return String(cipher.doFinal(bytes))
        } catch (exception: IllegalBlockSizeException) {
            throw RuntimeException("Failed to decrypt password", exception)
        } catch (exception: BadPaddingException) {
            throw RuntimeException("Failed to decrypt password", exception)
        }

    }
}
