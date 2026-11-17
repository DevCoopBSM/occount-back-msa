package devcoop.occount.member.application.query

import devcoop.occount.member.application.usecase.contribution.ContributionDepositRequestResponse

data class ContributionDepositRequestListResponse(
    val requests: List<ContributionDepositRequestResponse>,
)

data class AdminContributionDepositRequestListResponse(
    val requests: List<AdminContributionDepositRequestResponse>,
    val page: PageMeta,
)
