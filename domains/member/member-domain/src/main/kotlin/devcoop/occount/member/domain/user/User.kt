package devcoop.occount.member.domain.user

import java.time.LocalDate

data class User(
    private val id: Long = 0L,
    private val userInfo: UserInfo,
    private val accountInfo: AccountInfo,
    private val userSensitiveInfo: UserSensitiveInfo,
) {
    fun getRole(): Role = accountInfo.role
    fun getUsername(): String = userInfo.username
    fun getPhone(): String? = userInfo.phone
    fun getUserType(): UserType = userInfo.userType
    fun getCooperativeNumber(): String? = userInfo.cooperativeNumber
    fun getId(): Long = id
    fun getUserPin(): String = accountInfo.pin
    fun getUserBarcode(): String? = userInfo.userBarcode
    fun getBirthDate(): LocalDate? = userInfo.birthDate
    fun getStudentNumber(): String? = userInfo.studentNumber
    fun getEmail(): String = accountInfo.email
    fun getPassword(): String = accountInfo.password
    fun getCiNumber(): String? = userSensitiveInfo.ciNumber

    fun matchesPassword(rawPassword: String, matches: (String, String) -> Boolean): Boolean =
        accountInfo.matchesPassword(rawPassword, matches)

    fun matchesPin(rawPin: String, matches: (String, String) -> Boolean): Boolean =
        accountInfo.matchesPin(rawPin, matches)

    fun withBarcode(barcode: String): User = copy(userInfo = userInfo.copy(userBarcode = barcode))

    fun changePassword(encodedPassword: String): User =
        copy(accountInfo = accountInfo.copy(password = encodedPassword))

    fun changePin(encodedPin: String): User =
        copy(accountInfo = accountInfo.copy(pin = encodedPin))

    companion object {
        fun register(
            userCiNumber: String,
            username: String,
            phone: String?,
            email: String,
            encodedPassword: String,
            encodedPin: String,
            birthDate: LocalDate? = null,
        ): User {
            return User(
                userInfo = UserInfo(
                    username = username,
                    phone = phone,
                    userType = UserType.STUDENT,
                    cooperativeNumber = null,
                    userBarcode = null,
                    birthDate = birthDate,
                ),
                accountInfo = AccountInfo(
                    email = email,
                    password = encodedPassword,
                    role = Role.ROLE_USER,
                    pin = encodedPin,
                ),
                userSensitiveInfo = UserSensitiveInfo(
                    ciNumber = userCiNumber,
                ),
            )
        }
    }
}
