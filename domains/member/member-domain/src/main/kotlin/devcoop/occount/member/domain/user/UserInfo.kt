package devcoop.occount.member.domain.user

data class UserInfo(
    val username: String,
    val phone: String?,
    val userType: UserType,
    val cooperativeNumber: String?,
    val userBarcode: String?,
)
