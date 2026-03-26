package devcoop.occount.member.domain.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("User 도메인 단위 테스트")
class UserTest {

    private fun createFullUser(id: Long = 0L) = User(
        id = id,
        userInfo = UserInfo(
            username = "홍길동",
            phone = "010-1234-5678",
            userType = UserType.STUDENT,
            cooperativeNumber = "COOP001",
            userBarcode = "BARCODE123",
        ),
        accountInfo = AccountInfo(
            email = "test@test.com",
            password = "encodedPassword",
            role = Role.ROLE_USER,
            pin = "encodedPin",
        ),
        userSensitiveInfo = UserSensitiveInfo(ciNumber = "CI123456"),
    )

    @Test
    @DisplayName("보조 생성자로 User를 생성하면 UserType이 STUDENT, Role이 ROLE_USER로 기본 설정된다")
    fun `secondary constructor sets default userType and role`() {
        val user = User(
            userCiNumber = "CI123",
            username = "홍길동",
            phone = "010-1234-5678",
            userEmail = "test@test.com",
            encodedPassword = "encodedPassword",
            encodedPin = "encodedPin",
        )

        assertEquals(UserType.STUDENT, user.getUserType())
        assertEquals(Role.ROLE_USER, user.getRole())
    }

    @Test
    @DisplayName("보조 생성자로 생성된 User는 바코드와 조합원 번호가 null이다")
    fun `secondary constructor sets barcode and cooperativeNumber to null`() {
        val user = User(
            userCiNumber = "CI123",
            username = "홍길동",
            phone = null,
            userEmail = "test@test.com",
            encodedPassword = "encodedPassword",
            encodedPin = "encodedPin",
        )

        assertNull(user.getUserBarcode())
        assertNull(user.getCooperativeNumber())
    }

    @Test
    @DisplayName("id가 명시되지 않으면 기본값 0으로 초기화된다")
    fun `default id is zero when not provided`() {
        val user = createFullUser()

        assertEquals(0L, user.getId())
    }

    @Test
    @DisplayName("주 생성자로 생성된 User는 모든 필드를 올바르게 반환한다")
    fun `primary constructor exposes all fields correctly`() {
        val user = createFullUser(id = 42L)

        assertEquals(42L, user.getId())
        assertEquals("홍길동", user.getUsername())
        assertEquals("010-1234-5678", user.getPhone())
        assertEquals("test@test.com", user.getEmail())
        assertEquals("encodedPassword", user.getPassword())
        assertEquals("encodedPin", user.getUserPin())
        assertEquals("CI123456", user.getCiNumber())
        assertEquals("BARCODE123", user.getUserBarcode())
        assertEquals("COOP001", user.getCooperativeNumber())
        assertEquals(Role.ROLE_USER, user.getRole())
        assertEquals(UserType.STUDENT, user.getUserType())
    }

    @Test
    @DisplayName("전화번호가 null일 때 getPhone()이 null을 반환한다")
    fun `getPhone returns null when phone is null`() {
        val user = User(
            userCiNumber = "CI123",
            username = "홍길동",
            phone = null,
            userEmail = "test@test.com",
            encodedPassword = "encodedPassword",
            encodedPin = "encodedPin",
        )

        assertNull(user.getPhone())
    }

    @Test
    @DisplayName("CI번호가 null일 때 getCiNumber()가 null을 반환한다")
    fun `getCiNumber returns null when ciNumber is null`() {
        val user = User(
            userInfo = UserInfo("홍길동", null, UserType.STUDENT, null, null),
            accountInfo = AccountInfo("test@test.com", "pw", Role.ROLE_USER, "pin"),
            userSensitiveInfo = UserSensitiveInfo(ciNumber = null),
        )

        assertNull(user.getCiNumber())
    }
}
