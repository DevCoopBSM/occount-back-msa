package devcoop.occount.db.user

import devcoop.occount.member.domain.user.UserType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class UserInfoJpaEmbeddable(
    @field:Column(nullable = false)
    private var username: String = "",
    @field:Column(unique = true)
    private var phone: String? = null,
    @field:Column(unique = true)
    private var userBarcode: String? = null,
    @field:Column(nullable = false)
    private var userType: UserType = UserType.STUDENT,
    @field:Column(unique = true)
    private var cooperativeNumber: String? = null,
) {
    fun getUsername() = username
    fun getPhone() = phone
    fun getUserBarcode() = userBarcode
    fun getUserType() = userType
    fun getCooperativeNumber() = cooperativeNumber
}
