package devcoop.occount.member.application.query

import devcoop.occount.member.application.support.FakeContributionAccountRepository
import devcoop.occount.member.domain.contribution.ContributionAccount
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GetMyContributionAccountQueryService 단위 테스트")
class GetMyContributionAccountQueryServiceTest {
    @Test
    @DisplayName("내 출자금 잔액을 조회한다")
    fun `getAccount returns my contribution balance`() {
        val queryService = GetMyContributionAccountQueryService(
            FakeContributionAccountRepository(
                listOf(ContributionAccount(userId = 1L, balance = 10000)),
            ),
        )

        val response = queryService.getAccount(userId = 1L)

        assertEquals(10000, response.balance)
    }

    @Test
    @DisplayName("출자금 계좌가 없으면 잔액 0을 반환한다")
    fun `getAccount returns zero when account does not exist`() {
        val queryService = GetMyContributionAccountQueryService(FakeContributionAccountRepository())

        val response = queryService.getAccount(userId = 1L)

        assertEquals(0, response.balance)
    }
}
