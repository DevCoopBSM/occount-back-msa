package devcoop.occount.member.infrastructure.portone

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PortOneProperties 단위 테스트")
class PortOnePropertiesTest {
    @Test
    @DisplayName("secretKey 설정 값을 보존한다")
    fun `preserves secret key`() {
        assertEquals("portone-secret", PortOneProperties(secretKey = "portone-secret").secretKey)
    }
}
