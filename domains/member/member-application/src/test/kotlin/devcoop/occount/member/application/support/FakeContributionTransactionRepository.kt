package devcoop.occount.member.application.support

import devcoop.occount.member.application.output.ContributionTransactionRepository
import devcoop.occount.member.domain.contribution.ContributionTransaction

class FakeContributionTransactionRepository : ContributionTransactionRepository {
    val saved = mutableListOf<ContributionTransaction>()

    override fun save(transaction: ContributionTransaction): ContributionTransaction {
        val persisted = if (transaction.id == 0L) transaction.copy(id = saved.size + 1L) else transaction
        saved += persisted
        return persisted
    }

    override fun findAllByUserId(userId: Long): List<ContributionTransaction> {
        return saved.filter { it.userId == userId }
            .sortedByDescending { it.occurredAt }
    }
}
