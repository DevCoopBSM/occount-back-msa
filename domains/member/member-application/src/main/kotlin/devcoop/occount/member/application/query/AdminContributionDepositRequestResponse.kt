package devcoop.occount.member.application.query

import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus
import devcoop.occount.member.domain.user.User
import java.time.LocalDateTime

data class AdminContributionDepositRequestResponse(
    val id: Long,
    val userId: Long,
    val username: String?,
    val userEmail: String?,
    val amount: Int,
    val depositorName: String?,
    val bankName: String?,
    val proofImageUrl: String?,
    val memo: String?,
    val status: ContributionDepositRequestStatus,
    val rejectionReason: String?,
    val requestedAt: LocalDateTime,
) {
    companion object {
        fun from(request: ContributionDepositRequest, user: User?): AdminContributionDepositRequestResponse {
            return AdminContributionDepositRequestResponse(
                id = request.id,
                userId = request.userId,
                username = user?.getUsername(),
                userEmail = user?.getEmail(),
                amount = request.amount,
                depositorName = request.depositorName,
                bankName = request.bankName,
                proofImageUrl = request.proofImageUrl,
                memo = request.memo,
                status = request.status,
                rejectionReason = request.rejectionReason,
                requestedAt = request.requestedAt,
            )
        }
    }
}
