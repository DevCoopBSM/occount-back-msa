package devcoop.occount.member.domain.user

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("User 도메인 단위 테스트")
class UserTest {
    @Test
    @DisplayName("회원 등록 시 기본 유저 타입과 권한을 가진다")
    fun `register creates member with default type and role`() {
        val user = User.register(
            userCiNumber = "CI123",
            username = "홍길동",
            phone = "010-1234-5678",
            email = "test@test.com",
            encodedPassword = "encoded:password1234",
            encodedPin = "encoded:000000",
        )

        assertEquals(UserType.STUDENT, user.getUserType())
        assertEquals(Role.ROLE_USER, user.getRole())
        assertNull(user.getUserBarcode())
        assertNull(user.getCooperativeNumber())
    }

    @Test
    @DisplayName("회원 로그인 비밀번호가 일치하면 true를 반환한다")
    fun `matchesPassword returns true for matching password`() {
        val user = userFixture()

        val result = user.matchesPassword("password1234", ::matchesEncodedValue)

        assertTrue(result)
    }

    @Test
    @DisplayName("회원 로그인 비밀번호가 다르면 false를 반환한다")
    fun `matchesPassword returns false for mismatched password`() {
        val user = userFixture()

        val result = user.matchesPassword("wrong-password", ::matchesEncodedValue)

        assertFalse(result)
    }

    @Test
    @DisplayName("키오스크 핀번호가 일치하면 true를 반환한다")
    fun `matchesPin returns true for matching pin`() {
        val user = userFixture()

        val result = user.matchesPin("123456", ::matchesEncodedValue)

        assertTrue(result)
    }

    @Test
    @DisplayName("키오스크 핀번호가 다르면 false를 반환한다")
    fun `matchesPin returns false for mismatched pin`() {
        val user = userFixture()

        val result = user.matchesPin("654321", ::matchesEncodedValue)

        assertFalse(result)
    }

    private fun userFixture(): User =
        User.register(
            userCiNumber = "CI123",
            username = "홍길동",
            phone = "010-1234-5678",
            email = "test@test.com",
            encodedPassword = "encoded:password1234",
            encodedPin = "encoded:123456",
        ).withBarcode("BARCODE123").copy(id = 1L)

    private fun matchesEncodedValue(raw: String, encoded: String): Boolean {
        return encoded == "encoded:$raw"
    }
}
