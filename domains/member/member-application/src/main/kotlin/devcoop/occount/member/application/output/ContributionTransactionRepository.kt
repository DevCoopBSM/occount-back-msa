package devcoop.occount.member.application.output

import devcoop.occount.member.domain.contribution.ContributionTransaction

interface ContributionTransactionRepository {
    fun save(transaction: ContributionTransaction): ContributionTransaction
    fun findAllByUserId(userId: Long): List<ContributionTransaction>
}
