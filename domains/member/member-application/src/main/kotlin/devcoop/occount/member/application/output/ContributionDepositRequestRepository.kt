package devcoop.occount.member.application.output

import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus

interface ContributionDepositRequestRepository {
    fun findById(id: Long): ContributionDepositRequest?
    fun save(request: ContributionDepositRequest): ContributionDepositRequest
    fun findPage(status: ContributionDepositRequestStatus?, page: Int, size: Int): PageResult<ContributionDepositRequest>
    fun findAllByUserId(userId: Long): List<ContributionDepositRequest>
}
