package devcoop.occount.member.domain.contribution

import java.time.LocalDateTime

data class ContributionTransaction(
    val id: Long = 0L,
    val userId: Long,
    val type: ContributionTransactionType,
    val amount: Long,
    val beforeBalance: Long,
    val afterBalance: Long,
    val sourceRequestId: Long,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun deposit(
            userId: Long,
            amount: Long,
            beforeBalance: Long,
            afterBalance: Long,
            sourceRequestId: Long,
        ): ContributionTransaction {
            return ContributionTransaction(
                userId = userId,
                type = ContributionTransactionType.DEPOSIT,
                amount = amount,
                beforeBalance = beforeBalance,
                afterBalance = afterBalance,
                sourceRequestId = sourceRequestId,
            )
        }

        fun withdrawal(
            userId: Long,
            amount: Long,
            beforeBalance: Long,
            afterBalance: Long,
            sourceRequestId: Long,
        ): ContributionTransaction {
            return ContributionTransaction(
                userId = userId,
                type = ContributionTransactionType.WITHDRAWAL,
                amount = amount,
                beforeBalance = beforeBalance,
                afterBalance = afterBalance,
                sourceRequestId = sourceRequestId,
            )
        }
    }
}
