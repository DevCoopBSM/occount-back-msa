package devcoop.occount.member.application.query

import devcoop.occount.member.domain.user.Role
import devcoop.occount.member.domain.user.User
import devcoop.occount.member.domain.user.UserType
import java.time.LocalDate

/**
 * 관리자 사용자 목록의 항목 응답.
 * 비밀번호/PIN/CI 등 민감정보는 절대 포함하지 않는다.
 */
data class AdminUserSummaryResponse(
    val id: Long,
    val username: String,
    val email: String,
    val phone: String?,
    val userType: UserType,
    val role: Role,
    val cooperativeNumber: String?,
    val userBarcode: String?,
    val birthDate: LocalDate?,
) {
    companion object {
        fun from(user: User): AdminUserSummaryResponse {
            return AdminUserSummaryResponse(
                id = user.getId(),
                username = user.getUsername(),
                email = user.getEmail(),
                phone = user.getPhone(),
                userType = user.getUserType(),
                role = user.getRole(),
                cooperativeNumber = user.getCooperativeNumber(),
                userBarcode = user.getUserBarcode(),
                birthDate = user.getBirthDate(),
            )
        }
    }
}
