package devcoop.occount.member.domain.contribution

import java.time.LocalDateTime

data class ContributionDepositRequest(
    val id: Long = 0L,
    val userId: Long,
    val amount: Int,
    val depositorName: String? = null,
    val bankName: String? = null,
    val proofImageUrl: String? = null,
    val memo: String? = null,
    val status: ContributionDepositRequestStatus = ContributionDepositRequestStatus.PENDING,
    val rejectionReason: String? = null,
    val requestedAt: LocalDateTime = LocalDateTime.now(),
    val version: Long = 0L,
) {
    init {
        if (amount <= 0) {
            throw InvalidContributionDepositAmountException()
        }
    }

    fun approve(): ContributionDepositRequest {
        validatePending()
        return copy(status = ContributionDepositRequestStatus.APPROVED)
    }

    fun reject(reason: String? = null): ContributionDepositRequest {
        validatePending()
        return copy(
            status = ContributionDepositRequestStatus.REJECTED,
            rejectionReason = reason,
        )
    }

    private fun validatePending() {
        if (status != ContributionDepositRequestStatus.PENDING) {
            throw ContributionDepositRequestAlreadyProcessedException()
        }
    }

    companion object {
        fun request(
            userId: Long,
            amount: Int,
            depositorName: String? = null,
            bankName: String? = null,
            proofImageUrl: String? = null,
            memo: String? = null,
        ): ContributionDepositRequest {
            return ContributionDepositRequest(
                userId = userId,
                amount = amount,
                depositorName = depositorName,
                bankName = bankName,
                proofImageUrl = proofImageUrl,
                memo = memo,
            )
        }
    }
}
