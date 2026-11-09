package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.output.ContributionDepositRequestRepository
import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import org.springframework.stereotype.Service

@Service
class SubmitContributionDepositRequestUseCase(
    private val contributionDepositRequestRepository: ContributionDepositRequestRepository,
) {
    fun submit(userId: Long, request: SubmitContributionDepositRequest): ContributionDepositRequestResponse {
        val depositRequest = ContributionDepositRequest.request(
            userId = userId,
            amount = request.amount,
            depositorName = request.depositorName,
            bankName = request.bankName,
            proofImageUrl = request.proofImageUrl,
            memo = request.memo,
        )

        return contributionDepositRequestRepository.save(depositRequest)
            .let(ContributionDepositRequestResponse::from)
    }
}
