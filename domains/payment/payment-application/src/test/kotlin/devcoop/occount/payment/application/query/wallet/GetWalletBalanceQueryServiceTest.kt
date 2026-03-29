package devcoop.occount.payment.application.query.wallet

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.support.FakeWalletRepository
import devcoop.occount.payment.domain.Wallet
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetWalletBalanceQueryServiceTest {
    @Test
    @DisplayName("잔액 조회 시 기존 포인트 지갑의 잔액을 반환한다")
    fun `getBalance returns existing point wallet balance`() {
        val repository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, balance = 50)),
        )
        val queryService = GetWalletBalanceQueryService(repository)

        val response = queryService.getBalance(1L)

        assertEquals(WalletBalanceResponse(balance = 50), response)
    }

    @Test
    @DisplayName("잔액 조회 시 포인트 지갑이 없으면 WalletNotFound가 발생한다")
    fun `getBalance throws WalletNotFound when wallet does not exist`() {
        val repository = FakeWalletRepository()
        val queryService = GetWalletBalanceQueryService(repository)

        assertFailsWith<WalletNotFoundException> {
            queryService.getBalance(1L)
        }
    }
}
