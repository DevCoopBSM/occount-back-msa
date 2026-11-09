package devcoop.occount.member.domain.contribution

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ContributionDepositRequest(
    val id: Long = 0L,
    val userId: Long,
    val amount: Long,
    val depositorName: String? = null,
    val bankName: String? = null,
    val proofImageUrl: String? = null,
    val memo: String? = null,
    val status: ContributionDepositRequestStatus = ContributionDepositRequestStatus.PENDING,
    val rejectionReason: String? = null,
    val requestedAt: LocalDateTime = LocalDateTime.now(),
    val processedBy: Long? = null,
    val processedAt: LocalDateTime? = null,
    val version: Long? = null,
) {
    init {
        if (amount <= 0) {
            throw InvalidContributionDepositAmountException()
        }
    }

    fun approve(processedBy: Long): ContributionDepositRequest {
        validatePending()
        return copy(status = ContributionDepositRequestStatus.APPROVED, processedBy = processedBy, processedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
    }

    fun reject(processedBy: Long, reason: String? = null): ContributionDepositRequest {
        validatePending()
        return copy(
            status = ContributionDepositRequestStatus.REJECTED,
            rejectionReason = reason,
            processedBy = processedBy,
            processedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS),
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
            amount: Long,
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
