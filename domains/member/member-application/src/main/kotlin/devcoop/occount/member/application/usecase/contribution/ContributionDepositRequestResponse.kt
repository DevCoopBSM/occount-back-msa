package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus
import java.time.LocalDateTime

data class ContributionDepositRequestResponse(
    val id: Long,
    val userId: Long,
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
        fun from(request: ContributionDepositRequest): ContributionDepositRequestResponse {
            return ContributionDepositRequestResponse(
                id = request.id,
                userId = request.userId,
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
