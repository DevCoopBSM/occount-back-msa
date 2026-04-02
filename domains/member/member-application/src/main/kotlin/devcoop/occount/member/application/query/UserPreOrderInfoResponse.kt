package devcoop.occount.member.application.query

import devcoop.occount.member.domain.user.User

data class UserPreOrderInfoResponse(
    val username: String,
    val point: Int,
) {
    companion object {
        fun toUserPreOrderInfoResponse(user: User, point: Int): UserPreOrderInfoResponse {
            return UserPreOrderInfoResponse(
                username = user.getUsername(),
                point = point,
            )
        }
    }
}
