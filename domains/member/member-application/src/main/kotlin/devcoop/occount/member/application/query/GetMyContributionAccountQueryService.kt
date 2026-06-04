package devcoop.occount.member.application.query

import devcoop.occount.member.application.output.ContributionAccountRepository
import devcoop.occount.member.domain.contribution.ContributionAccount
import org.springframework.stereotype.Service

@Service
class GetMyContributionAccountQueryService(
    private val contributionAccountRepository: ContributionAccountRepository,
) {
    fun getAccount(userId: Long): ContributionAccountResponse {
        val account = contributionAccountRepository.findByUserId(userId)
            ?: ContributionAccount(userId = userId)
        return ContributionAccountResponse.from(account)
    }
}
