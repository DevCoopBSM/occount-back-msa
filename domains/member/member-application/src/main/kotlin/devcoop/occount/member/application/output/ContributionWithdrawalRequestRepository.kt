package devcoop.occount.member.application.output

import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequest
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequestStatus

interface ContributionWithdrawalRequestRepository {
    fun findById(id: Long): ContributionWithdrawalRequest?
    fun save(request: ContributionWithdrawalRequest): ContributionWithdrawalRequest
    fun findPage(status: ContributionWithdrawalRequestStatus?, page: Int, size: Int): PageResult<ContributionWithdrawalRequest>
    fun findAllByUserId(userId: Long): List<ContributionWithdrawalRequest>
}
