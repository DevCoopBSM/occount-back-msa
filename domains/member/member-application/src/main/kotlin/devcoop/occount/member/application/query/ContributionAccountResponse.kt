package devcoop.occount.member.application.query

import devcoop.occount.member.domain.contribution.ContributionAccount

data class ContributionAccountResponse(
    val balance: Int,
) {
    companion object {
        fun from(account: ContributionAccount): ContributionAccountResponse {
            return ContributionAccountResponse(
                balance = account.balance,
            )
        }
    }
}
