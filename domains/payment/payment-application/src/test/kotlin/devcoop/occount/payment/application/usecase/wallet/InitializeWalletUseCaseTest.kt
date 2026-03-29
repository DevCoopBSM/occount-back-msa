package devcoop.occount.payment.application.usecase.wallet

import devcoop.occount.payment.application.exception.WalletAlreadyInitializedException
import devcoop.occount.payment.application.support.FakeWalletRepository
import devcoop.occount.payment.domain.Wallet
import org.junit.jupiter.api.DisplayName
import org.springframework.dao.DuplicateKeyException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InitializeWalletUseCaseTest {
    @Test
    @DisplayName("포인트 지갑 초기화 시 새 지갑을 저장한다")
    fun `initialize saves new point wallet`() {
        val repository = FakeWalletRepository()
        val useCase = InitializeWalletUseCase(repository)

        useCase.initialize(1L)

        assertEquals(1, repository.savedWallets.size)
        assertEquals(Wallet(userId = 1L, balance = 0), repository.savedWallets.single())
    }

    @Test
    @DisplayName("포인트 지갑 초기화 시 중복 키 예외는 WalletAlreadyInitializedException으로 변환된다")
    fun `initialize converts duplicate key exception to WalletAlreadyInitializedException`() {
        val repository = FakeWalletRepository(
            saveException = DuplicateKeyException("duplicate"),
        )
        val useCase = InitializeWalletUseCase(repository)

        assertFailsWith<WalletAlreadyInitializedException> {
            useCase.initialize(1L)
        }
    }
}
