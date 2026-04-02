package devcoop.occount.member.application.query

import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.application.output.WalletPointReader
import org.springframework.stereotype.Service

@Service
class UserPreOrderInfoQueryService(
    private val userRepository: UserRepository,
    private val walletPointReader: WalletPointReader,
) {
    fun findPreOrderInfo(userId: Long): UserPreOrderInfoResponse {
        val user = userRepository.findById(userId)
            ?: throw UserNotFoundException()
        val point = walletPointReader.getPoint(userId)

        return UserPreOrderInfoResponse.toUserPreOrderInfoResponse(user, point)
    }
}
