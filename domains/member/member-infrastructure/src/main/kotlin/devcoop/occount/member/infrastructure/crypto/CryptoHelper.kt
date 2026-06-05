package devcoop.occount.member.infrastructure.crypto

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoHelper(
    secretKey: String,
) {
    private val secretKeySpec: SecretKeySpec

    init {
        val keyBytes = secretKey.toByteArray(StandardCharsets.UTF_8)
        require(keyBytes.size == AES_256_KEY_BYTES) {
            "app.encryption.secret-key must be 32 bytes in UTF-8 for AES-256."
        }
        secretKeySpec = SecretKeySpec(keyBytes, AES)
    }

    fun encrypt(plainText: String?): String? {
        if (plainText.isNullOrEmpty()) {
            return plainText
        }

        val iv = ByteArray(IV_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, GCMParameterSpec(GCM_TAG_BITS, iv))

        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val encryptedData = iv + cipherText
        return ENCRYPTION_PREFIX + Base64.getEncoder().encodeToString(encryptedData)
    }

    fun decrypt(encryptedText: String?): String? {
        if (encryptedText.isNullOrEmpty()) {
            return encryptedText
        }
        require(encryptedText.startsWith(ENCRYPTION_PREFIX)) {
            // Plaintext fallback would silently expose mixed legacy data as valid application data.
            "Encrypted value must start with $ENCRYPTION_PREFIX. Legacy plaintext data requires migration."
        }

        val encryptedData = Base64.getDecoder().decode(encryptedText.removePrefix(ENCRYPTION_PREFIX))
        require(encryptedData.size > IV_BYTES) {
            "Encrypted value is too short."
        }

        val iv = encryptedData.copyOfRange(0, IV_BYTES)
        val cipherText = encryptedData.copyOfRange(IV_BYTES, encryptedData.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, GCMParameterSpec(GCM_TAG_BITS, iv))

        return String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
    }

    companion object {
        const val ENCRYPTION_PREFIX = "ENC:"
        private const val AES = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_256_KEY_BYTES = 32
        private const val IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private val secureRandom = SecureRandom()
    }
}
