package devcoop.occount.member.application.query

import devcoop.occount.member.application.output.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class AdminUserQueryService(
    private val userRepository: UserRepository,
) {
    fun findAllUsers(pageable: Pageable): AdminUserListResponse {
        val page = userRepository.findAll(pageable)
        return AdminUserListResponse(
            users = page.content.map(AdminUserSummaryResponse::from),
            totalCount = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            pageSize = page.size,
        )
    }
}
