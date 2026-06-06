package devcoop.occount.member.infrastructure.crypto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Base64

@DisplayName("CryptoHelper 단위 테스트")
class CryptoHelperTest {
    private val cryptoHelper = CryptoHelper("12345678901234567890123456789012")

    @Test
    @DisplayName("평문을 암호화하면 ENC prefix가 붙고 복호화하면 원문을 반환한다")
    fun `encrypt returns prefixed ciphertext and decrypt restores plaintext`() {
        val plainText = "010-1234-5678"

        val encryptedText = cryptoHelper.encrypt(plainText)

        assertNotEquals(plainText, encryptedText)
        assertTrue(encryptedText!!.startsWith(CryptoHelper.ENCRYPTION_PREFIX))
        assertTrue(cryptoHelper.isEncrypted(encryptedText))
        assertEquals(plainText, cryptoHelper.decrypt(encryptedText))
    }

    @Test
    @DisplayName("ENC prefix가 있어도 유효한 AES-GCM envelope가 아니면 암호문으로 판단하지 않는다")
    fun `isEncrypted returns false for invalid encrypted envelope`() {
        assertFalse(cryptoHelper.isEncrypted("ENC:alice"))
        assertFalse(cryptoHelper.isEncrypted("ENC:not-base64!"))
        assertFalse(cryptoHelper.isEncrypted("plainText"))
    }

    @Test
    @DisplayName("길이가 충분한 Base64 envelope라도 GCM 복호화에 실패하면 암호문으로 판단하지 않는다")
    fun `isEncrypted returns false for envelope that cannot be decrypted`() {
        val invalidEnvelope = ByteArray(29) { index -> index.toByte() }
        val invalidEncryptedText = CryptoHelper.ENCRYPTION_PREFIX +
            Base64.getEncoder().encodeToString(invalidEnvelope)

        assertFalse(cryptoHelper.isEncrypted(invalidEncryptedText))
    }

    @Test
    @DisplayName("같은 평문을 두 번 암호화하면 랜덤 IV 때문에 암호문이 다르다")
    fun `encrypt uses random iv`() {
        val plainText = "CI123456"

        val firstEncryptedText = cryptoHelper.encrypt(plainText)
        val secondEncryptedText = cryptoHelper.encrypt(plainText)

        assertNotEquals(firstEncryptedText, secondEncryptedText)
        assertEquals(plainText, cryptoHelper.decrypt(firstEncryptedText))
        assertEquals(plainText, cryptoHelper.decrypt(secondEncryptedText))
    }

    @Test
    @DisplayName("null과 empty string은 그대로 반환한다")
    fun `encrypt and decrypt preserve null and empty string`() {
        assertNull(cryptoHelper.encrypt(null))
        assertNull(cryptoHelper.decrypt(null))
        assertEquals("", cryptoHelper.encrypt(""))
        assertEquals("", cryptoHelper.decrypt(""))
    }
}
