package devcoop.occount.member.infrastructure.crypto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UniqueEncryptedValueGenerator 단위 테스트")
class UniqueEncryptedValueGeneratorTest {
    @Test
    @DisplayName("생성한 암호문이 이미 존재하면 원문을 다시 암호화해 다른 값을 반환한다")
    fun `generate retries when encrypted value already exists`() {
        val encryptedValues = ArrayDeque(listOf("ENC:duplicated", "ENC:unique"))
        val generator = UniqueEncryptedValueGenerator { encryptedValues.removeFirst() }

        val result = generator.generate("010-1234-5678") { it == "ENC:duplicated" }

        assertEquals("ENC:unique", result)
    }

    @Test
    @DisplayName("null과 empty string은 암호문을 생성하지 않는다")
    fun `generate preserves null and empty string`() {
        val generator = UniqueEncryptedValueGenerator { error("encrypt must not be called") }

        assertNull(generator.generate(null) { false })
        assertEquals("", generator.generate("") { false })
    }
}
