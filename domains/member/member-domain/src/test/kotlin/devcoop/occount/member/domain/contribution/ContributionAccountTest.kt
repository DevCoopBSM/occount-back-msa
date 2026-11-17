package devcoop.occount.member.domain.contribution

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ContributionAccount 도메인 테스트")
class ContributionAccountTest {
    @Test
    fun `deposit supports Long limits and rejects overflow without mutating balance`() {
        val account = ContributionAccount(userId = 1L, balance = Long.MAX_VALUE - 1)
        assertEquals(Long.MAX_VALUE, account.deposit(1L).balance)
        assertFailsWith<ContributionBalanceOverflowException> { account.deposit(2L) }
        assertEquals(Long.MAX_VALUE - 1, account.balance)
    }

    @Test
    fun `withdrawal consumes exact balance and rejects invalid or insufficient amounts`() {
        val account = ContributionAccount(userId = 1L, balance = 3_000_000_000L)
        assertEquals(0L, account.withdraw(3_000_000_000L).balance)
        assertFailsWith<InsufficientContributionBalanceException> { account.withdraw(3_000_000_001L) }
        assertFailsWith<InvalidContributionWithdrawalAmountException> { account.withdraw(0L) }
        assertFailsWith<InvalidContributionWithdrawalAmountException> { account.withdraw(-1L) }
    }


    @Test
    @DisplayName("출자금 계좌에 납입 금액을 더한다")
    fun `deposit increases balance`() {
        val account = ContributionAccount(userId = 1L, balance = 10000)

        val updated = account.deposit(5000)

        assertEquals(15000L, updated.balance)
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
