package devcoop.occount.member.application.output

import devcoop.occount.member.domain.contribution.ContributionAccount

interface ContributionAccountRepository {
    fun findByUserId(userId: Long): ContributionAccount?
    fun save(account: ContributionAccount): ContributionAccount
}
