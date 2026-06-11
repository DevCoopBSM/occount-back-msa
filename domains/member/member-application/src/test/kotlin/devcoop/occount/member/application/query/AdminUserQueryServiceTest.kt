package devcoop.occount.member.application.query

import devcoop.occount.member.application.support.userFixture
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.User
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

@DisplayName("AdminUserQueryService 단위 테스트")
class AdminUserQueryServiceTest {
    @Test
    @DisplayName("23명을 size 10으로 0페이지 조회하면 10명과 페이징 메타(total 23, totalPages 3)를 반환한다")
    fun `findAllUsers returns first page with paging meta`() {
        val users = (1L..23L).map { userFixture(id = it, email = "user$it@test.com") }
        val service = AdminUserQueryService(PagingFakeUserRepository(users))

        val result = service.findAllUsers(PageRequest.of(0, 10))

        assertEquals(10, result.users.size)
        assertEquals(23L, result.totalCount)
        assertEquals(3, result.totalPages)
        assertEquals(0, result.currentPage)
        assertEquals(10, result.pageSize)
    }

    @Test
    @DisplayName("마지막 페이지는 남은 인원만큼만 반환한다")
    fun `findAllUsers returns remaining users on last page`() {
        val users = (1L..23L).map { userFixture(id = it, email = "user$it@test.com") }
        val service = AdminUserQueryService(PagingFakeUserRepository(users))

        val result = service.findAllUsers(PageRequest.of(2, 10))

        assertEquals(3, result.users.size)
        assertEquals(2, result.currentPage)
    }

    @Test
    @DisplayName("응답 항목은 User 도메인의 안전 필드만 매핑한다")
    fun `findAllUsers maps safe user fields`() {
        val user = userFixture(id = 7L, username = "홍길동", email = "hong@test.com", barcode = "BARCODE-7")
        val service = AdminUserQueryService(PagingFakeUserRepository(listOf(user)))

        val summary = service.findAllUsers(PageRequest.of(0, 10)).users.single()

        assertEquals(7L, summary.id)
        assertEquals("홍길동", summary.username)
        assertEquals("hong@test.com", summary.email)
        assertEquals("BARCODE-7", summary.userBarcode)
    }

    @Test
    @DisplayName("AdminUserSummaryResponse에는 민감필드(password/pin/ciNumber)가 구조적으로 존재하지 않는다")
    fun `summary response has no sensitive properties`() {
        val propertyNames = AdminUserSummaryResponse::class.members.map { it.name }.toSet()

        assertTrue("password" !in propertyNames)
        assertTrue("pin" !in propertyNames)
        assertTrue("userPin" !in propertyNames)
        assertTrue("ciNumber" !in propertyNames)
    }
}

private class PagingFakeUserRepository(
    private val users: List<User>,
) : UserRepository {
    override fun findById(id: Long): User? = users.firstOrNull { it.getId() == id }
    override fun findByUserBarcode(userBarcode: String): User? = users.firstOrNull { it.getUserBarcode() == userBarcode }
    override fun findByEmail(userEmail: String): User? = users.firstOrNull { it.getEmail() == userEmail }
    override fun existsByEmail(userEmail: String): Boolean = users.any { it.getEmail() == userEmail }
    override fun save(user: User): User = user

    override fun findAll(pageable: Pageable): Page<User> {
        val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(users.size)
        val to = (from + pageable.pageSize).coerceAtMost(users.size)
        return PageImpl(users.subList(from, to), pageable, users.size.toLong())
    }
}
