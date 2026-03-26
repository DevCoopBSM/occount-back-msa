package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.domain.user.Role
import devcoop.occount.member.domain.user.UserType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "common_user")
class UserJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @field:Column(nullable = false)
    val username: String = "",
    @field:Column(unique = true)
    val phone: String? = null,
    @field:Column(unique = true)
    val userBarcode: String? = null,
    @Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    val userType: UserType = UserType.STUDENT,
    @field:Column(unique = true)
    val cooperativeNumber: String? = null,
    @field:Column(nullable = false, unique = true)
    val userEmail: String = "",
    @field:Column(nullable = false)
    val userPassword: String = "",
    @Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    val role: Role = Role.ROLE_USER,
    @field:Column(nullable = false)
    val pin: String = "",
    @field:Column(unique = true)
    val userCiNumber: String? = null,
)
