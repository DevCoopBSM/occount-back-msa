package devcoop.occount.member.application.user

import devcoop.occount.member.domain.user.User

data class UserPreOrderInfoResponse(
    val username: String,
) {
    companion object {
        fun toUserPreOrderInfoResponse(user: User): UserPreOrderInfoResponse {
            return UserPreOrderInfoResponse(
                username = user.getUsername(),
            )
        }
    }
}
