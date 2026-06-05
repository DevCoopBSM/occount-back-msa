package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.infrastructure.crypto.CryptoConverter
import devcoop.occount.member.domain.user.Role
import devcoop.occount.member.domain.user.UserType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "common_user")
class UserJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Convert(converter = CryptoConverter::class)
    @field:Column(nullable = false)
    val username: String = "",
    @Convert(converter = CryptoConverter::class)
    @field:Column
    val phone: String? = null,
    @field:Column(unique = true)
    val userBarcode: String? = null,
    @Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    val userType: UserType = UserType.STUDENT,
    @field:Column(unique = true)
    val cooperativeNumber: String? = null,
    @field:Column(nullable = false, unique = true)
    val email: String = "",
    @field:Column(nullable = false)
    val password: String = "",
    @Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    val role: Role = Role.ROLE_USER,
    @field:Column(nullable = false)
    val pin: String = "",
    @Convert(converter = CryptoConverter::class)
    @field:Column
    val userCiNumber: String? = null,
    @field:Column
    val birthDate: LocalDate? = null,
)
