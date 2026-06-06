package devcoop.occount.member.infrastructure.crypto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SensitiveInformationHasher 단위 테스트")
class SensitiveInformationHasherTest {
    private val hasher = SensitiveInformationHasher("12345678901234567890123456789012")

    @Test
    @DisplayName("같은 평문은 같은 HMAC 해시를 반환한다")
    fun `hash returns same value for same plaintext`() {
        val firstHash = hasher.hash("010-1234-5678")
        val secondHash = hasher.hash("010-1234-5678")

        assertEquals(firstHash, secondHash)
        assertEquals(64, firstHash!!.length)
    }

    @Test
    @DisplayName("다른 평문은 다른 HMAC 해시를 반환한다")
    fun `hash returns different value for different plaintext`() {
        assertNotEquals(hasher.hash("010-1234-5678"), hasher.hash("010-0000-0000"))
    }

    @Test
    @DisplayName("null과 empty string은 검색 해시를 만들지 않는다")
    fun `hash returns null for null and empty string`() {
        assertNull(hasher.hash(null))
        assertNull(hasher.hash(""))
    }
}
