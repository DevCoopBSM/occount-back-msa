package devcoop.occount.member.domain.contribution

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ContributionAccount 도메인 테스트")
class ContributionAccountTest {
    @Test
    @DisplayName("출자금 계좌에 납입 금액을 더한다")
    fun `deposit increases balance`() {
        val account = ContributionAccount(userId = 1L, balance = 10000)

        val updated = account.deposit(5000)

        assertEquals(15000, updated.balance)
    }

    @Test
    @DisplayName("출자금 계좌 납입 금액은 0보다 커야 한다")
    fun `deposit amount must be positive`() {
        val account = ContributionAccount(userId = 1L)

        assertFailsWith<InvalidContributionDepositAmountException> {
            account.deposit(0)
        }
    }
}
