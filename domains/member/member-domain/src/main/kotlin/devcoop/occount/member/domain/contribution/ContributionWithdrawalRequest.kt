package devcoop.occount.member.domain.contribution

import java.time.LocalDateTime

data class ContributionWithdrawalRequest(
    val id: Long = 0L,
    val userId: Long,
    val amount: Int,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val accountHolder: String? = null,
    val memo: String? = null,
    val status: ContributionWithdrawalRequestStatus = ContributionWithdrawalRequestStatus.PENDING,
    val rejectionReason: String? = null,
    val requestedAt: LocalDateTime = LocalDateTime.now(),
    val version: Long? = null,
) {
    init {
        if (amount <= 0) {
            throw InvalidContributionWithdrawalAmountException()
        }
    }

    fun approve(): ContributionWithdrawalRequest {
        validatePending()
        return copy(status = ContributionWithdrawalRequestStatus.APPROVED)
    }

    fun reject(reason: String? = null): ContributionWithdrawalRequest {
        validatePending()
        return copy(
            status = ContributionWithdrawalRequestStatus.REJECTED,
            rejectionReason = reason,
        )
    }

    private fun validatePending() {
        if (status != ContributionWithdrawalRequestStatus.PENDING) {
            throw ContributionWithdrawalRequestAlreadyProcessedException()
        }
    }

    companion object {
        fun request(
            userId: Long,
            amount: Int,
            bankName: String? = null,
            accountNumber: String? = null,
            accountHolder: String? = null,
            memo: String? = null,
        ): ContributionWithdrawalRequest {
            return ContributionWithdrawalRequest(
                userId = userId,
                amount = amount,
                bankName = bankName,
                accountNumber = accountNumber,
                accountHolder = accountHolder,
                memo = memo,
            )
        }
    }
}
