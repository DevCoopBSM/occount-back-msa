package devcoop.occount.db.user

import devcoop.occount.member.domain.user.Role
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
class UserAccountInfoJpaEmbeddable(
    @field:Column(nullable = false, unique = true)
    private var userEmail: String = "",
    @field:Column(nullable = false)
    private var userPassword: String = "",
    @Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    private var role: Role = Role.ROLE_USER,
    @field:Column(nullable = false)
    private var pin: String = "",
) {
    fun getUserEmail() = userEmail
    fun getUserPassword() = userPassword
    fun getRole() = role
    fun getPin() = pin
}
