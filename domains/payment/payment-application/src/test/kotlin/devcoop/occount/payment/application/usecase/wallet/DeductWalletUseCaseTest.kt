package devcoop.occount.payment.application.usecase.wallet

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.query.wallet.WalletResponse
import devcoop.occount.payment.application.support.FakeWalletRepository
import devcoop.occount.payment.application.usecase.wallet.deduct.DeductWalletUseCase
import devcoop.occount.payment.domain.wallet.Wallet
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeductWalletUseCaseTest {
    @Test
    @DisplayName("차감 시 변경된 포인트를 저장하고 잔액을 반환한다")
    fun `deduct saves updated point wallet`() {
        val repository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 100)),
        )
        val useCase = DeductWalletUseCase(repository)

        val response = useCase.deduct(1L, 25)

        assertEquals(WalletResponse(beforePoint = 100, afterPoint = 75), response)
        assertEquals(Wallet(userId = 1L, point = 75), repository.savedWallets.single())
    }

    @Test
    @DisplayName("차감 시 포인트 지갑이 없으면 WalletNotFound가 발생한다")
    fun `deduct throws WalletNotFound when wallet does not exist`() {
        val repository = FakeWalletRepository()
        val useCase = DeductWalletUseCase(repository)

        assertFailsWith<WalletNotFoundException> {
            useCase.deduct(1L, 10)
        }
    }
}
