package devcoop.occount.member.domain.user

data class User(
    private val id: Long = 0L,
    val userInfo: UserInfo,
    val accountInfo: AccountInfo,
    val userSensitiveInfo: UserSensitiveInfo,
) {
    constructor(
        userCiNumber: String,
        username: String,
        phone: String?,
        email: String,
        encodedPassword: String,
        encodedPin: String,
    ) : this(
        userInfo = UserInfo(
            username = username,
            phone = phone,
            userType = UserType.STUDENT,
            cooperativeNumber = null,
            userBarcode = null,
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

    fun getRole(): Role {
        return accountInfo.role
    }

    fun getUsername(): String {
        return userInfo.username
    }

    fun getPhone(): String? {
        return userInfo.phone
    }

    fun getUserType(): UserType {
        return userInfo.userType
    }

    fun getCooperativeNumber(): String? {
        return userInfo.cooperativeNumber
    }

    fun getId(): Long {
        return id
    }

    fun getUserPin(): String {
        return accountInfo.pin
    }

    fun getUserBarcode(): String? {
        return userInfo.userBarcode
    }

    fun getEmail(): String {
        return accountInfo.email
    }

    fun getPassword(): String {
        return accountInfo.password
    }

    fun getCiNumber(): String? {
        return userSensitiveInfo.ciNumber
    }
}
