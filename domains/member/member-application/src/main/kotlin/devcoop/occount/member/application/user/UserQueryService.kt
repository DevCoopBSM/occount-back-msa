package devcoop.occount.member.application.user

import org.springframework.stereotype.Service

@Service
class UserQueryService(
    private val userRepository: UserRepository,
) {
    fun findPreOrderInfo(userId: Long): UserPreOrderInfoResponse {
        val user = userRepository.findById(userId)
            ?: throw UserNotFoundException()

        return UserPreOrderInfoResponse.toUserPreOrderInfoResponse(user)
    }
}
