package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.support.FakeContributionDepositRequestRepository
import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RejectContributionDepositRequestUseCase 단위 테스트")
class RejectContributionDepositRequestUseCaseTest {
    @Test
    @DisplayName("출자금 납입 신청을 반려 상태로 변경한다")
    fun `reject changes request to rejected`() {
        val repository = FakeContributionDepositRequestRepository(
            listOf(ContributionDepositRequest.request(userId = 1L, amount = 10000)),
        )
        val useCase = RejectContributionDepositRequestUseCase(repository)

        val response = useCase.reject(requestId = 1L)

        assertEquals(ContributionDepositRequestStatus.REJECTED, response.status)
    }
}
