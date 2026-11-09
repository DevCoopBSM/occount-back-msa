package devcoop.occount.payment.application.usecase.wallet

import devcoop.occount.payment.application.support.FakeChargeLogRepository
import devcoop.occount.payment.application.support.FakeWalletRepository
import devcoop.occount.payment.application.usecase.wallet.admincharge.AdminChargeCommand
import devcoop.occount.payment.application.usecase.wallet.admincharge.AdminChargeStatus
import devcoop.occount.payment.application.usecase.wallet.admincharge.AdminChargeWalletUseCase
import devcoop.occount.payment.application.usecase.wallet.admincharge.WalletChargeProcessor
import devcoop.occount.payment.domain.wallet.ChargeReason
import devcoop.occount.payment.domain.wallet.Wallet
import org.junit.jupiter.api.DisplayName
import org.springframework.dao.OptimisticLockingFailureException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminChargeWalletUseCaseTest {
    private fun useCaseWith(
        walletRepository: FakeWalletRepository,
        chargeLogRepository: FakeChargeLogRepository,
    ): AdminChargeWalletUseCase =
        AdminChargeWalletUseCase(WalletChargeProcessor(walletRepository, chargeLogRepository))

    @Test
    @DisplayName("여러 사용자에게 충전하면 각 지갑에 금액이 더해지고 모두 성공으로 보고된다")
    fun `charges multiple wallets and reports all charged`() {
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(
                1L to Wallet(userId = 1L, point = 100),
                2L to Wallet(userId = 2L, point = 0),
            ),
        )
        val chargeLogRepository = FakeChargeLogRepository()
        val useCase = useCaseWith(walletRepository, chargeLogRepository)

        val result = useCase.charge(
            AdminChargeCommand(adminId = 99L, userIds = listOf(1L, 2L), amount = 500, reason = "5월 우수회원 보상"),
        )

        assertEquals(2, result.chargedCount)
        assertEquals(0, result.failedCount)
        assertEquals(600, walletRepository.findByUserId(1L)!!.point)
        assertEquals(500, walletRepository.findByUserId(2L)!!.point)
        assertEquals(setOf(AdminChargeStatus.CHARGED), result.results.map { it.status }.toSet())
    }

    @Test
    @DisplayName("지갑이 없는 사용자는 실패로 분리되고 나머지는 충전된다 (부분 성공)")
    fun `reports partial success when some wallets are missing`() {
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 0)),
        )
        val chargeLogRepository = FakeChargeLogRepository()
        val useCase = useCaseWith(walletRepository, chargeLogRepository)

        val result = useCase.charge(
            AdminChargeCommand(adminId = 99L, userIds = listOf(1L, 404L), amount = 1000, reason = "이벤트"),
        )

        assertEquals(1, result.chargedCount)
        assertEquals(1, result.failedCount)
        assertEquals(1000, walletRepository.findByUserId(1L)!!.point)

        val charged = result.results.single { it.status == AdminChargeStatus.CHARGED }
        assertEquals(1L, charged.userId)
        assertNull(charged.message)

        val failed = result.results.single { it.status == AdminChargeStatus.FAILED }
        assertEquals(404L, failed.userId)
        assertEquals("WALLET_NOT_FOUND", failed.message)

        // 실패 사용자에 대한 충전 로그는 남지 않는다
        assertEquals(listOf(1L), chargeLogRepository.savedLogs.map { it.userId })
    }

    @Test
    @DisplayName("충전 로그에 ADMIN 사유, 상세 사유, 충전한 어드민 id가 기록된다")
    fun `records admin reason detail and charger id in charge log`() {
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 0)),
        )
        val chargeLogRepository = FakeChargeLogRepository()
        val useCase = useCaseWith(walletRepository, chargeLogRepository)

        useCase.charge(
            AdminChargeCommand(adminId = 77L, userIds = listOf(1L), amount = 300, reason = "지급 보상"),
        )

        val log = chargeLogRepository.savedLogs.single()
        assertEquals(ChargeReason.ADMIN, log.chargeReason)
        assertEquals("지급 보상", log.detailReason)
        assertEquals(77L, log.chargedBy)
        assertEquals(0, log.pointTransaction.beforePoint)
        assertEquals(300, log.pointTransaction.changeAmount)
        assertEquals(300, log.pointTransaction.afterPoint)
    }

    @Test
    @DisplayName("중복된 사용자 id는 한 번만 충전한다")
    fun `deduplicates repeated user ids`() {
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 0)),
        )
        val chargeLogRepository = FakeChargeLogRepository()
        val useCase = useCaseWith(walletRepository, chargeLogRepository)

        val result = useCase.charge(
            AdminChargeCommand(adminId = 1L, userIds = listOf(1L, 1L, 1L), amount = 100, reason = "이벤트"),
        )

        assertEquals(1, result.results.size)
        assertEquals(100, walletRepository.findByUserId(1L)!!.point)
        assertEquals(1, chargeLogRepository.savedLogs.size)
    }

    @Test
    @DisplayName("동시 수정 충돌이 나면 해당 사용자만 실패로 격리한다")
    fun `isolates user on optimistic locking failure`() {
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 0)),
            saveException = OptimisticLockingFailureException("concurrent update"),
        )
        val chargeLogRepository = FakeChargeLogRepository()
        val useCase = useCaseWith(walletRepository, chargeLogRepository)

        val result = useCase.charge(
            AdminChargeCommand(adminId = 1L, userIds = listOf(1L), amount = 100, reason = "이벤트"),
        )

        assertEquals(0, result.chargedCount)
        assertEquals(1, result.failedCount)
        val failed = result.results.single()
        assertEquals(1L, failed.userId)
        assertEquals("CONCURRENT_MODIFICATION", failed.message)
    }
}
