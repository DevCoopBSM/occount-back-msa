package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequest
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequestStatus
import java.time.LocalDateTime

data class ContributionWithdrawalRequestResponse(
    val id: Long,
    val userId: Long,
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
        fun from(request: ContributionWithdrawalRequest): ContributionWithdrawalRequestResponse {
            return ContributionWithdrawalRequestResponse(
                id = request.id,
                userId = request.userId,
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
