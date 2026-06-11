package devcoop.occount.member.api.user

import devcoop.occount.member.application.query.AdminUserListResponse
import devcoop.occount.member.application.query.AdminUserQueryService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 전용 사용자 조회/검색.
 * 인가는 게이트웨이(GET /api/v3/users → ADMIN_ONLY)에서만 강제하며 다운스트림 재검증은 하지 않는다.
 */
@RestController
@RequestMapping("/users")
class AdminUserController(
    private val adminUserQueryService: AdminUserQueryService,
) {
    companion object {
        private const val MAX_PAGE_SIZE = 100
        private val ALLOWED_SORT_FIELDS = setOf("id")
        private val DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "id")
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun findAllUsers(
        @RequestParam(required = false) keyword: String?,
        @PageableDefault(size = 10, sort = ["id"]) pageable: Pageable,
    ): AdminUserListResponse {
        return adminUserQueryService.findAllUsers(keyword, sanitizePageable(pageable))
    }

    private fun sanitizePageable(pageable: Pageable): Pageable {
        val cappedSize = minOf(pageable.pageSize, MAX_PAGE_SIZE)
        val filteredOrders = pageable.sort.filter { it.property in ALLOWED_SORT_FIELDS }.toList()
        val sanitizedSort = if (filteredOrders.isEmpty()) DEFAULT_SORT else Sort.by(filteredOrders)
        return PageRequest.of(pageable.pageNumber, cappedSize, sanitizedSort)
    }
}
