package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.support.FakeContributionDepositRequestRepository
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SubmitContributionDepositRequestUseCase 단위 테스트")
class SubmitContributionDepositRequestUseCaseTest {
    @Test
    @DisplayName("사용자가 출자금 납입 신청을 생성한다")
    fun `submit creates contribution deposit request`() {
        val repository = FakeContributionDepositRequestRepository()
        val useCase = SubmitContributionDepositRequestUseCase(repository)

        val response = useCase.submit(
            userId = 1L,
            request = SubmitContributionDepositRequest(amount = 10000),
        )

        val savedRequest = repository.findAllByUserId(1L).single()
        assertEquals(savedRequest.id, response.id)
        assertEquals(10000, response.amount)
        assertEquals(ContributionDepositRequestStatus.PENDING, response.status)
    }
}
