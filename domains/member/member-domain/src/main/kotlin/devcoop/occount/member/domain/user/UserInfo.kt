package devcoop.occount.member.domain.user

import java.time.LocalDate

data class UserInfo(
    val username: String,
    val phone: String?,
    val userType: UserType,
    val cooperativeNumber: String?,
    val userBarcode: String?,
    val birthDate: LocalDate?,
)
