package devcoop.occount.member.application.query

import devcoop.occount.member.application.support.matchesKeyword
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

        val result = service.findAllUsers(keyword = null, pageable = PageRequest.of(0, 10))

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

        val result = service.findAllUsers(keyword = null, pageable = PageRequest.of(2, 10))

        assertEquals(3, result.users.size)
        assertEquals(2, result.currentPage)
    }

    @Test
    @DisplayName("응답 항목은 User 도메인의 안전 필드만 매핑한다")
    fun `findAllUsers maps safe user fields`() {
        val user = userFixture(id = 7L, username = "홍길동", email = "hong@test.com", barcode = "BARCODE-7")
        val service = AdminUserQueryService(PagingFakeUserRepository(listOf(user)))

        val summary = service.findAllUsers(keyword = null, pageable = PageRequest.of(0, 10)).users.single()

        assertEquals(7L, summary.id)
        assertEquals("홍길동", summary.username)
        assertEquals("hong@test.com", summary.email)
        assertEquals("BARCODE-7", summary.userBarcode)
    }

    @Test
    @DisplayName("keyword가 주어지면 이름/이메일/조합원번호 부분일치로 검색한다")
    fun `findAllUsers searches by keyword across name email and cooperative number`() {
        val users = listOf(
            userFixture(id = 1L, username = "김철수", email = "chulsoo@test.com"),
            userFixture(id = 2L, username = "이영희", email = "younghee@test.com"),
            userFixture(id = 3L, username = "박철수", email = "park@test.com"),
            userFixture(id = 4L, username = "최민수", email = "min@test.com", cooperativeNumber = "COOP-777"),
        )
        val service = AdminUserQueryService(PagingFakeUserRepository(users))

        val byName = service.findAllUsers(keyword = "철수", pageable = PageRequest.of(0, 10))
        val byEmail = service.findAllUsers(keyword = "younghee", pageable = PageRequest.of(0, 10))
        val byCoop = service.findAllUsers(keyword = "COOP-777", pageable = PageRequest.of(0, 10))

        assertEquals(setOf(1L, 3L), byName.users.map { it.id }.toSet())
        assertEquals(1L, byEmail.totalCount)
        assertEquals(listOf(2L), byEmail.users.map { it.id })
        assertEquals(listOf(4L), byCoop.users.map { it.id })
    }

    @Test
    @DisplayName("keyword가 공백이거나 null이면 전체 조회로 동작한다")
    fun `findAllUsers falls back to full list when keyword is blank or null`() {
        val users = (1L..5L).map { userFixture(id = it, email = "user$it@test.com") }
        val service = AdminUserQueryService(PagingFakeUserRepository(users))

        val blank = service.findAllUsers(keyword = "   ", pageable = PageRequest.of(0, 10))
        val nullKeyword = service.findAllUsers(keyword = null, pageable = PageRequest.of(0, 10))

        assertEquals(5L, blank.totalCount)
        assertEquals(5L, nullKeyword.totalCount)
    }

    @Test
    @DisplayName("LIKE 메타문자가 포함된 keyword는 리터럴 부분일치로만 매칭한다")
    fun `findAllUsers matches keyword literally even with like metacharacters`() {
        val users = listOf(
            userFixture(id = 1L, username = "50%할인", email = "sale@test.com"),
            userFixture(id = 2L, username = "5000원", email = "won@test.com"),
        )
        val service = AdminUserQueryService(PagingFakeUserRepository(users))

        val result = service.findAllUsers(keyword = "50%", pageable = PageRequest.of(0, 10))

        assertEquals(listOf(1L), result.users.map { it.id })
    }

    @Test
    @DisplayName("검색 결과도 페이징 메타를 정확히 반환한다")
    fun `findAllUsers keeps paging meta on search results`() {
        val users = (1L..15L).map { userFixture(id = it, username = "코딩$it", email = "user$it@test.com") }
        val service = AdminUserQueryService(PagingFakeUserRepository(users))

        val result = service.findAllUsers(keyword = "코딩", pageable = PageRequest.of(0, 10))

        assertEquals(10, result.users.size)
        assertEquals(15L, result.totalCount)
        assertEquals(2, result.totalPages)
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

    override fun searchByKeyword(keyword: String, pageable: Pageable): Page<User> {
        val matched = users.filter { it.matchesKeyword(keyword) }
        val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(matched.size)
        val to = (from + pageable.pageSize).coerceAtMost(matched.size)
        return PageImpl(matched.subList(from, to), pageable, matched.size.toLong())
    }
}
