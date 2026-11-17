package devcoop.occount.member.application.support

import devcoop.occount.member.application.output.ContributionAccountRepository
import devcoop.occount.member.domain.contribution.ContributionAccount

class FakeContributionAccountRepository(
    initialAccounts: List<ContributionAccount> = emptyList(),
) : ContributionAccountRepository {
    private val accountsByUserId = linkedMapOf<Long, ContributionAccount>().apply {
        initialAccounts.forEach { account -> put(account.userId, account) }
    }

    override fun findByUserId(userId: Long): ContributionAccount? = accountsByUserId[userId]

    override fun findByUserIdForUpdate(userId: Long): ContributionAccount? = findByUserId(userId)

    override fun promoteOwnerToMember(userId: Long) = Unit

    override fun save(account: ContributionAccount): ContributionAccount {
        accountsByUserId[account.userId] = account
        return account
    }
}
