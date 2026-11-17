package devcoop.occount.member.domain.contribution

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ContributionWithdrawalRequest(
    val id: Long = 0L,
    val userId: Long,
    val amount: Long,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val accountHolder: String? = null,
    val memo: String? = null,
    val status: ContributionWithdrawalRequestStatus = ContributionWithdrawalRequestStatus.PENDING,
    val rejectionReason: String? = null,
    val requestedAt: LocalDateTime = LocalDateTime.now(),
    val processedBy: Long? = null,
    val processedAt: LocalDateTime? = null,
    val version: Long? = null,
) {
    init {
        if (amount <= 0) {
            throw InvalidContributionWithdrawalAmountException()
        }
    }

    fun approve(processedBy: Long): ContributionWithdrawalRequest {
        validatePending()
        return copy(status = ContributionWithdrawalRequestStatus.APPROVED, processedBy = processedBy, processedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
    }

    fun reject(processedBy: Long, reason: String? = null): ContributionWithdrawalRequest {
        validatePending()
        return copy(
            status = ContributionWithdrawalRequestStatus.REJECTED,
            rejectionReason = reason,
            processedBy = processedBy,
            processedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS),
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
            amount: Long,
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
