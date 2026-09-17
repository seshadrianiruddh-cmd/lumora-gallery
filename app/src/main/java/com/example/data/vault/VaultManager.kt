package com.example.data.vault

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.database.VaultItemEntity
import java.io.File
import javax.crypto.SecretKey

class VaultManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("lumora_vault_prefs", Context.MODE_PRIVATE)

    // In-memory master key active session
    private var activeMasterKey: SecretKey? = null

    fun isVaultSetup(): Boolean {
        return prefs.contains("encrypted_master_key") && prefs.contains("salt") && prefs.contains("iv")
    }

    fun isUnlocked(): Boolean = activeMasterKey != null

    fun lock() {
        activeMasterKey = null
    }

    /**
     * Initializes the Vault with a new PIN.
     * Generates a random master key and encrypts it using a key derived from the user's PIN.
     */
    fun setupVault(pin: String): Boolean {
        try {
            val salt = VaultCrypto.generateSalt()
            val masterKey = VaultCrypto.generateMasterKey()
            val pinDerivedKey = VaultCrypto.deriveKeyFromPin(pin, salt)

            val (encryptedMasterKey, iv) = VaultCrypto.encryptMasterKey(masterKey, pinDerivedKey)

            prefs.edit()
                .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                .putString("encrypted_master_key", Base64.encodeToString(encryptedMasterKey, Base64.NO_WRAP))
                .apply()

            activeMasterKey = masterKey
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Attempts to unlock the Vault with the user's PIN.
     */
    fun unlockVault(pin: String): Boolean {
        try {
            val saltStr = prefs.getString("salt", null) ?: return false
            val ivStr = prefs.getString("iv", null) ?: return false
            val encMasterKeyStr = prefs.getString("encrypted_master_key", null) ?: return false

            val salt = Base64.decode(saltStr, Base64.NO_WRAP)
            val iv = Base64.decode(ivStr, Base64.NO_WRAP)
            val encryptedMasterKey = Base64.decode(encMasterKeyStr, Base64.NO_WRAP)

            val pinDerivedKey = VaultCrypto.deriveKeyFromPin(pin, salt)
            val masterKey = VaultCrypto.decryptMasterKey(encryptedMasterKey, pinDerivedKey, iv)

            activeMasterKey = masterKey
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Changes the user's PIN without re-encrypting the vault media items!
     */
    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!unlockVault(oldPin)) return false
        val masterKey = activeMasterKey ?: return false

        try {
            val salt = VaultCrypto.generateSalt()
            val pinDerivedKey = VaultCrypto.deriveKeyFromPin(newPin, salt)

            val (encryptedMasterKey, iv) = VaultCrypto.encryptMasterKey(masterKey, pinDerivedKey)

            prefs.edit()
                .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                .putString("encrypted_master_key", Base64.encodeToString(encryptedMasterKey, Base64.NO_WRAP))
                .apply()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Encrypts and imports a media item into internal storage.
     */
    fun importToVault(
        sourceUri: Uri,
        originalName: String,
        mimeType: String,
        fileSize: Long,
        duration: Long,
        width: Int,
        height: Int
    ): VaultItemEntity? {
        val masterKey = activeMasterKey ?: return null
        val vaultDir = File(context.filesDir, "secure_vault")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }

        // Generate a random secure filename in internal storage
        val uniqueFilename = "vault_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString() + ".enc"
        val destinationFile = File(vaultDir, uniqueFilename)

        try {
            val iv = VaultCrypto.encryptFile(context, sourceUri, destinationFile, masterKey)
            val ivHex = iv.joinToString("") { "%02x".format(it) }

            return VaultItemEntity(
                originalName = originalName,
                mimeType = mimeType,
                encryptedFilePath = destinationFile.absolutePath,
                ivHex = ivHex,
                duration = duration,
                width = width,
                height = height,
                fileSize = fileSize
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            return null
        }
    }

    /**
     * Decrypts a vault item into the output file or stream.
     */
    fun decryptVaultItem(vaultItem: VaultItemEntity, outputStream: java.io.OutputStream): Boolean {
        val masterKey = activeMasterKey ?: return false
        val file = File(vaultItem.encryptedFilePath)
        if (!file.exists()) return false

        try {
            // Parse IV hex back to byte array
            val iv = ByteArray(vaultItem.ivHex.length / 2)
            for (i in iv.indices) {
                val index = i * 2
                val j = vaultItem.ivHex.substring(index, index + 2).toInt(16)
                iv[i] = j.toByte()
            }

            VaultCrypto.decryptFile(file, outputStream, iv, masterKey)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun deleteEncryptedFile(vaultItem: VaultItemEntity): Boolean {
        val file = File(vaultItem.encryptedFilePath)
        return if (file.exists()) {
            file.delete()
        } else false
    }
}
