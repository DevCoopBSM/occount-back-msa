package devcoop.occount.member.application.query

import devcoop.occount.member.application.support.FakeContributionDepositRequestRepository
import devcoop.occount.member.application.support.FakeUserRepository
import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GetContributionDepositRequestsQueryService 단위 테스트")
class GetContributionDepositRequestsQueryServiceTest {
    @Test
    @DisplayName("관리자가 전체 출자금 납입 신청 목록을 조회한다")
    fun `findAll returns all contribution deposit requests`() {
        val repository = FakeContributionDepositRequestRepository(
            listOf(
                ContributionDepositRequest.request(userId = 1L, amount = 10000),
                ContributionDepositRequest.request(userId = 2L, amount = 20000).approve(),
            ),
        )
        val queryService = GetContributionDepositRequestsQueryService(repository, FakeUserRepository())

        val response = queryService.findAll(status = null, page = 0, size = 20)

        assertEquals(2, response.requests.size)
    }

    @Test
    @DisplayName("관리자가 상태별 출자금 납입 신청 목록을 조회한다")
    fun `findAll filters contribution deposit requests by status`() {
        val repository = FakeContributionDepositRequestRepository(
            listOf(
                ContributionDepositRequest.request(userId = 1L, amount = 10000),
                ContributionDepositRequest.request(userId = 2L, amount = 20000).approve(),
            ),
        )
        val queryService = GetContributionDepositRequestsQueryService(repository, FakeUserRepository())

        val response = queryService.findAll(status = ContributionDepositRequestStatus.PENDING, page = 0, size = 20)

        assertEquals(1, response.requests.size)
        assertEquals(ContributionDepositRequestStatus.PENDING, response.requests.single().status)
        assertEquals(1L, response.requests.single().userId)
    }
}
