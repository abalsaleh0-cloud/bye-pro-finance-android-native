package com.byepro.bye_pro_finance.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Device-local application gate.
 *
 * Passwords are never stored as readable text. A random salt and PBKDF2-HMAC-SHA256
 * verifier are persisted in app_settings. This is not a remote user account service.
 */
class LocalAuthentication(
    private val readSetting: (String) -> String?,
    private val writeSetting: (String, String) -> Unit
) {
    fun initializeIfNeeded() {
        val storedSalt = readSetting(KEY_SALT)
        val storedHash = readSetting(KEY_HASH)
        if (storedSalt.isNullOrBlank() || storedHash.isNullOrBlank()) {
            val salt = newSalt()
            writeSetting(KEY_USERNAME, DEFAULT_USERNAME)
            writeSetting(KEY_SALT, Base64.getEncoder().encodeToString(salt))
            writeSetting(KEY_HASH, encodeHash(derive(DEFAULT_PASSWORD, salt)))
            writeSetting(KEY_ENABLED, "true")
        } else {
            if (readSetting(KEY_USERNAME).isNullOrBlank()) {
                writeSetting(KEY_USERNAME, DEFAULT_USERNAME)
            }
            if (readSetting(KEY_ENABLED) == null) {
                writeSetting(KEY_ENABLED, "true")
            }
        }
    }

    fun isEnabled(): Boolean = readSetting(KEY_ENABLED) != "false"

    fun setEnabled(enabled: Boolean) {
        writeSetting(KEY_ENABLED, enabled.toString())
    }

    fun currentUsername(): String =
        readSetting(KEY_USERNAME)?.takeIf { it.isNotBlank() } ?: DEFAULT_USERNAME

    fun authenticate(username: String, password: String): Boolean {
        if (!isEnabled()) return true
        if (username.trim() != currentUsername()) return false
        return verifyPassword(password)
    }

    fun verifyPassword(password: String): Boolean {
        val salt = decodeSalt(readSetting(KEY_SALT)) ?: return false
        val expected = decodeHash(readSetting(KEY_HASH)) ?: return false
        val actual = derive(password, salt)
        return MessageDigest.isEqual(expected, actual)
    }

    /**
     * Changes both login fields only after the current password is verified.
     * New passwords must contain at least six characters.
     */
    fun changeCredentials(
        currentPassword: String,
        newUsername: String,
        newPassword: String
    ): CredentialChangeResult {
        val cleanUsername = newUsername.trim()
        if (!verifyPassword(currentPassword)) {
            return CredentialChangeResult.CURRENT_PASSWORD_INVALID
        }
        if (cleanUsername.length < MIN_USERNAME_LENGTH) {
            return CredentialChangeResult.USERNAME_TOO_SHORT
        }
        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            return CredentialChangeResult.PASSWORD_TOO_SHORT
        }

        val salt = newSalt()
        writeSetting(KEY_USERNAME, cleanUsername)
        writeSetting(KEY_SALT, Base64.getEncoder().encodeToString(salt))
        writeSetting(KEY_HASH, encodeHash(derive(newPassword, salt)))
        return CredentialChangeResult.SUCCESS
    }

    private fun newSalt(): ByteArray =
        ByteArray(SALT_BYTES).also(random::nextBytes)

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(KDF_ALGORITHM)
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encodeHash(hash: ByteArray): String =
        Base64.getEncoder().encodeToString(hash)

    private fun decodeSalt(encoded: String?): ByteArray? =
        runCatching { encoded?.let(Base64.getDecoder()::decode) }.getOrNull()
            ?.takeIf { it.size == SALT_BYTES }

    private fun decodeHash(encoded: String?): ByteArray? =
        runCatching { encoded?.let(Base64.getDecoder()::decode) }.getOrNull()
            ?.takeIf { it.size == KEY_BITS / 8 }

    enum class CredentialChangeResult {
        SUCCESS,
        CURRENT_PASSWORD_INVALID,
        USERNAME_TOO_SHORT,
        PASSWORD_TOO_SHORT
    }

    companion object {
        const val DEFAULT_USERNAME = "admin"
        const val DEFAULT_PASSWORD = "admin"

        const val KEY_USERNAME = "local_auth_username"
        const val KEY_SALT = "local_auth_salt"
        const val KEY_HASH = "local_auth_password_hash"
        const val KEY_ENABLED = "local_auth_enabled"

        const val MIN_USERNAME_LENGTH = 3
        const val MIN_PASSWORD_LENGTH = 6

        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 120_000
        private const val KEY_BITS = 256
        private const val SALT_BYTES = 16

        private val random = SecureRandom()
    }
}
