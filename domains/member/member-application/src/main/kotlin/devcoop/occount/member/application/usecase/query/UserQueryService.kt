package devcoop.occount.member.application.usecase.query

import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.output.UserRepository
import org.springframework.stereotype.Service

@Service
class UserQueryService(
    private val userRepository: UserRepository,
) {
    fun findPreOrderInfo(userId: Long): UserPreOrderInfoResponse {
        val user = userRepository.findById(userId)
            ?: throw UserNotFoundException()

        return UserPreOrderInfoResponse.Companion.toUserPreOrderInfoResponse(user)
    }
}
