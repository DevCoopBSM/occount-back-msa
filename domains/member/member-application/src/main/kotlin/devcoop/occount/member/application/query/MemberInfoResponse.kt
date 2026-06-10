package devcoop.occount.member.application.query

import devcoop.occount.member.domain.user.Role
import devcoop.occount.member.domain.user.User
import devcoop.occount.member.domain.user.UserType
import java.time.LocalDate

data class MemberInfoResponse(
    val username: String,
    val email: String,
    val phone: String?,
    val userType: UserType,
    val role: Role,
    val cooperativeNumber: String?,
    val birthDate: LocalDate?,
) {
    companion object {
        fun toMemberInfoResponse(user: User): MemberInfoResponse {
            return MemberInfoResponse(
                username = user.getUsername(),
                email = user.getEmail(),
                phone = user.getPhone(),
                userType = user.getUserType(),
                role = user.getRole(),
                cooperativeNumber = user.getCooperativeNumber(),
                birthDate = user.getBirthDate(),
            )
        }
    }
}
