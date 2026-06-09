package devcoop.occount.member.application.query

import devcoop.occount.member.domain.user.User

data class UserBarcodeResponse(
    val userBarcode: String?,
) {
    companion object {
        fun toUserBarcodeResponse(user: User): UserBarcodeResponse {
            return UserBarcodeResponse(
                userBarcode = user.getUserBarcode(),
            )
        }
    }
}
