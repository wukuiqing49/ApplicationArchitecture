package com.wkq.iptc.upload

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object UploadSecretStore {

    private const val TAG = "UploadSecretStore"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "field_camera_upload_secret_v1"
    private const val KEY_PASSWORD_CIPHER = "upload_password_secure_v1"
    private const val KEY_PASSWORD_LEGACY = "upload_password"
    private const val KEY_PROFILE_PASSWORD_PREFIX = "upload_profile_password_secure_"
    private const val KEY_HOST_FINGERPRINT_PREFIX = "upload_host_fingerprint_"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_SIZE_BYTES = 12

    fun loadPassword(): String {
        val cipherText = UploadPreferenceStore.getString(KEY_PASSWORD_CIPHER)
        if (cipherText.isNotBlank()) {
            return runCatching { decrypt(cipherText) }
                .onFailure {
                    Log.e(TAG, "loadPassword decrypt failed: ${it.message.orEmpty()}")
                }
                .getOrDefault("")
        }

        val legacy = UploadPreferenceStore.getString(KEY_PASSWORD_LEGACY)
        if (legacy.isBlank()) {
            return ""
        }
        savePassword(legacy)
        UploadPreferenceStore.remove(KEY_PASSWORD_LEGACY)
        return legacy
    }

    fun savePassword(password: String) {
        if (password.isBlank()) {
            clearPassword()
            return
        }
        runCatching {
            UploadPreferenceStore.putString(KEY_PASSWORD_CIPHER, encrypt(password))
            UploadPreferenceStore.remove(KEY_PASSWORD_LEGACY)
        }.onFailure {
            Log.e(TAG, "savePassword encrypt failed: ${it.message.orEmpty()}")
            throw IllegalStateException("Unable to securely store upload password", it)
        }
    }

    fun loadPassword(profileId: String): String {
        if (profileId.isBlank()) return ""
        val cipherText = UploadPreferenceStore.getString(profilePasswordKey(profileId))
        if (cipherText.isBlank()) return ""
        return runCatching { decrypt(cipherText) }
            .onFailure {
                Log.e(TAG, "loadPassword profile decrypt failed: ${it.message.orEmpty()}")
            }
            .getOrDefault("")
    }

    fun savePassword(profileId: String, password: String) {
        if (profileId.isBlank()) return
        if (password.isBlank()) {
            clearPassword(profileId)
            return
        }
        runCatching {
            UploadPreferenceStore.putString(profilePasswordKey(profileId), encrypt(password))
        }.onFailure {
            Log.e(TAG, "savePassword profile encrypt failed: ${it.message.orEmpty()}")
            throw IllegalStateException("Unable to securely store upload profile password", it)
        }
    }

    fun clearPassword(profileId: String) {
        if (profileId.isBlank()) return
        UploadPreferenceStore.remove(profilePasswordKey(profileId))
    }

    fun clearPassword() {
        UploadPreferenceStore.remove(KEY_PASSWORD_CIPHER)
        UploadPreferenceStore.remove(KEY_PASSWORD_LEGACY)
    }

    fun loadHostFingerprint(host: String, port: Int): String {
        return UploadPreferenceStore.getString(hostFingerprintKey(host, port))
    }

    fun saveHostFingerprint(host: String, port: Int, fingerprint: String) {
        if (fingerprint.isBlank()) return
        UploadPreferenceStore.putString(hostFingerprintKey(host, port), fingerprint)
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv ?: ByteArray(0)
        require(iv.size == IV_SIZE_BYTES) { "Unexpected GCM IV length: ${iv.size}" }
        val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encodedCipherText: String): String {
        val combined = Base64.decode(encodedCipherText, Base64.DEFAULT)
        require(combined.size > IV_SIZE_BYTES) { "Encrypted payload is invalid" }
        val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
        val cipherBytes = combined.copyOfRange(IV_SIZE_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )
        return cipher.doFinal(cipherBytes).toString(StandardCharsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun hostFingerprintKey(host: String, port: Int): String {
        return "$KEY_HOST_FINGERPRINT_PREFIX${host.lowercase()}:$port"
    }

    private fun profilePasswordKey(profileId: String): String {
        return "$KEY_PROFILE_PASSWORD_PREFIX$profileId"
    }
}


