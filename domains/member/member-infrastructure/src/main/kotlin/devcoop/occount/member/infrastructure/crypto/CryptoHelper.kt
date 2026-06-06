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
        val envelope = parseEncryptedEnvelope(encryptedText)
        require(envelope != null) {
            // Plaintext fallback would silently expose mixed legacy data as valid application data.
            "Encrypted value must be a valid AES-GCM envelope with $ENCRYPTION_PREFIX prefix."
        }

        return String(decryptEnvelope(envelope), StandardCharsets.UTF_8)
    }

    fun isEncrypted(value: String?): Boolean {
        val envelope = parseEncryptedEnvelope(value) ?: return false
        return runCatching { decryptEnvelope(envelope) }.isSuccess
    }

    private fun parseEncryptedEnvelope(value: String?): EncryptedEnvelope? {
        if (value.isNullOrEmpty() || !value.startsWith(ENCRYPTION_PREFIX)) {
            return null
        }
        return runCatching {
            val encryptedData = Base64.getDecoder().decode(value.removePrefix(ENCRYPTION_PREFIX))
            if (encryptedData.size <= IV_BYTES + GCM_TAG_BYTES) {
                return@runCatching null
            }
            EncryptedEnvelope(
                iv = encryptedData.copyOfRange(0, IV_BYTES),
                cipherText = encryptedData.copyOfRange(IV_BYTES, encryptedData.size),
            )
        }.getOrNull()
    }

    private fun decryptEnvelope(envelope: EncryptedEnvelope): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, GCMParameterSpec(GCM_TAG_BITS, envelope.iv))
        return cipher.doFinal(envelope.cipherText)
    }

    private data class EncryptedEnvelope(
        val iv: ByteArray,
        val cipherText: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is EncryptedEnvelope) {
                return false
            }
            return iv.contentEquals(other.iv) && cipherText.contentEquals(other.cipherText)
        }

        override fun hashCode(): Int {
            var result = iv.contentHashCode()
            result = 31 * result + cipherText.contentHashCode()
            return result
        }
    }

    companion object {
        const val ENCRYPTION_PREFIX = "ENC:"
        private const val AES = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_256_KEY_BYTES = 32
        private const val IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        private val secureRandom = SecureRandom()
    }
}
