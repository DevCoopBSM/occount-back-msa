package devcoop.occount.member.application.output

import devcoop.occount.member.domain.contribution.ContributionAccount

interface ContributionAccountRepository {
    fun findByUserId(userId: Long): ContributionAccount?
    // Lock the existing owner row, including when no contribution account exists yet.
    fun findByUserIdForUpdate(userId: Long): ContributionAccount?
    fun promoteOwnerToMember(userId: Long)
    fun save(account: ContributionAccount): ContributionAccount
}
