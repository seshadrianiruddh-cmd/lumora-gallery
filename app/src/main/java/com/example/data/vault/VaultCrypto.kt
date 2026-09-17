package com.example.data.vault

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BIT = 256

    /**
     * Derives an AES key from the user's PIN and a salt using PBKDF2.
     */
    fun deriveKeyFromPin(pin: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BIT)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, ALGORITHM)
    }

    /**
     * Generates a random cryptographically secure master key.
     */
    fun generateMasterKey(): SecretKey {
        val secureRandom = SecureRandom()
        val keyBytes = ByteArray(32) // 256 bits
        secureRandom.nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    /**
     * Generates a random salt.
     */
    fun generateSalt(size: Int = 16): ByteArray {
        val secureRandom = SecureRandom()
        val salt = ByteArray(size)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Encrypts the master key bytes using a PIN-derived key so it can be stored.
     * Returns a Pair of EncryptedMasterKeyBytes and IV.
     */
    fun encryptMasterKey(masterKey: SecretKey, pinDerivedKey: SecretKey): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, pinDerivedKey, spec)
        val encrypted = cipher.doFinal(masterKey.encoded)
        return Pair(encrypted, iv)
    }

    /**
     * Decrypts the stored master key bytes using a PIN-derived key.
     */
    fun decryptMasterKey(encryptedMasterKey: ByteArray, pinDerivedKey: SecretKey, iv: ByteArray): SecretKey {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, pinDerivedKey, spec)
        val decrypted = cipher.doFinal(encryptedMasterKey)
        return SecretKeySpec(decrypted, ALGORITHM)
    }

    /**
     * Encrypts a source file (accessed via Uri) and writes the encrypted data to internal storage.
     * Returns the 12-byte IV used.
     */
    fun encryptFile(
        context: Context,
        sourceUri: Uri,
        destinationFile: File,
        masterKey: SecretKey
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec)

        val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("Could not open input stream for URI: $sourceUri")

        inputStream.use { input ->
            FileOutputStream(destinationFile).use { fileOut ->
                CipherOutputStream(fileOut, cipher).use { cipherOut ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        cipherOut.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
        return iv
    }

    /**
     * Decrypts an encrypted file from internal storage and writes the decrypted data to an output stream.
     */
    fun decryptFile(
        encryptedFile: File,
        outputStream: OutputStream,
        iv: ByteArray,
        masterKey: SecretKey
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)

        FileInputStream(encryptedFile).use { fileIn ->
            CipherInputStream(fileIn, cipher).use { cipherIn ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (cipherIn.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
        }
    }
}
