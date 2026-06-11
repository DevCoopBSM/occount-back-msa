package devcoop.occount.member.application.query

import devcoop.occount.member.application.output.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class AdminUserQueryService(
    private val userRepository: UserRepository,
) {
    fun findAllUsers(keyword: String?, pageable: Pageable): AdminUserListResponse {
        val normalizedKeyword = keyword?.trim()?.takeIf { it.isNotEmpty() }
        val page = if (normalizedKeyword == null) {
            userRepository.findAll(pageable)
        } else {
            userRepository.searchByKeyword(normalizedKeyword, pageable)
        }
        return AdminUserListResponse(
            users = page.content.map(AdminUserSummaryResponse::from),
            totalCount = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            pageSize = page.size,
        )
    }
}
