package devcoop.occount.member.application.query

import devcoop.occount.member.application.usecase.contribution.ContributionWithdrawalRequestResponse
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequest
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequestStatus
import devcoop.occount.member.domain.user.User
import java.time.LocalDateTime

data class ContributionWithdrawalRequestListResponse(
    val requests: List<ContributionWithdrawalRequestResponse>,
)

data class AdminContributionWithdrawalRequestListResponse(
    val requests: List<AdminContributionWithdrawalRequestResponse>,
    val page: PageMeta,
)

data class AdminContributionWithdrawalRequestResponse(
    val id: Long,
    val userId: Long,
    val username: String?,
    val userEmail: String?,
    val amount: Int,
    val bankName: String?,
    val accountNumber: String?,
    val accountHolder: String?,
    val memo: String?,
    val status: ContributionWithdrawalRequestStatus,
    val rejectionReason: String?,
    val requestedAt: LocalDateTime,
) {
    companion object {
        fun from(request: ContributionWithdrawalRequest, user: User?): AdminContributionWithdrawalRequestResponse {
            return AdminContributionWithdrawalRequestResponse(
                id = request.id,
                userId = request.userId,
                username = user?.getUsername(),
                userEmail = user?.getEmail(),
                amount = request.amount,
                bankName = request.bankName,
                accountNumber = request.accountNumber,
                accountHolder = request.accountHolder,
                memo = request.memo,
                status = request.status,
                rejectionReason = request.rejectionReason,
                requestedAt = request.requestedAt,
            )
        }
    }
}
