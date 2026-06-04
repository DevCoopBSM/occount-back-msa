package devcoop.occount.member.application.query

import devcoop.occount.member.application.support.FakeContributionDepositRequestRepository
import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GetMyContributionDepositRequestsQueryService 단위 테스트")
class GetMyContributionDepositRequestsQueryServiceTest {
    @Test
    @DisplayName("내 출자금 납입 신청 목록만 조회한다")
    fun `findAll returns my contribution deposit requests`() {
        val repository = FakeContributionDepositRequestRepository(
            listOf(
                ContributionDepositRequest.request(userId = 1L, amount = 10000),
                ContributionDepositRequest.request(userId = 2L, amount = 20000),
            ),
        )
        val queryService = GetMyContributionDepositRequestsQueryService(repository)

        val response = queryService.findAll(userId = 1L)

        assertEquals(1, response.requests.size)
        assertEquals(10000, response.requests.single().amount)
    }
}
