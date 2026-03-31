package devcoop.occount.payment.application.query.wallet

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.support.FakeWalletRepository
import devcoop.occount.payment.domain.wallet.Wallet
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetWalletPointQueryServiceTest {
    @Test
    @DisplayName("잔액 조회 시 기존 포인트 지갑의 잔액을 반환한다")
    fun `getBalance returns existing point wallet balance`() {
        val repository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 50)),
        )
        val queryService = GetWalletPointQueryService(repository)

        val response = queryService.getPoint(1L)

        assertEquals(50, response)
    }

    @Test
    @DisplayName("잔액 조회 시 포인트 지갑이 없으면 WalletNotFound가 발생한다")
    fun `getBalance throws WalletNotFound when wallet does not exist`() {
        val repository = FakeWalletRepository()
        val queryService = GetWalletPointQueryService(repository)

        assertFailsWith<WalletNotFoundException> {
            queryService.getPoint(1L)
        }
    }
}
