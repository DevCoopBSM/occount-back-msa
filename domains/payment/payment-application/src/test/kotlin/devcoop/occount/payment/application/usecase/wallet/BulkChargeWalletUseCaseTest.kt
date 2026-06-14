package devcoop.occount.payment.application.usecase.wallet

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.application.support.FakeWalletRepository
import devcoop.occount.payment.application.usecase.wallet.charge.BulkChargeWalletRequest
import devcoop.occount.payment.application.usecase.wallet.charge.BulkChargeWalletUseCase
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.ChargeReason
import devcoop.occount.payment.domain.wallet.PointTransaction
import devcoop.occount.payment.domain.wallet.Wallet
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BulkChargeWalletUseCaseTest {
    @Test
    @DisplayName("여러 사용자의 포인트를 충전하고 사용자별 충전 기록을 저장한다")
    fun `charge increases points and saves logs for users`() {
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(
                1L to Wallet(userId = 1L, point = 100),
                2L to Wallet(userId = 2L, point = 250),
            ),
        )
        val chargeLogRepository = FakeChargeLogRepository()
        val useCase = BulkChargeWalletUseCase(walletRepository, chargeLogRepository)

        useCase.charge(
            BulkChargeWalletRequest(
                userIds = listOf(1L, 2L, 1L),
                amount = 50,
                reason = "체육대회 순위 상금",
            ),
        )

        assertEquals(
            listOf(
                Wallet(userId = 1L, point = 150),
                Wallet(userId = 2L, point = 300),
            ),
            walletRepository.savedWallets,
        )
        assertEquals(2, chargeLogRepository.savedChargeLogs.size)
        assertChargeLog(
            chargeLog = chargeLogRepository.savedChargeLogs[0],
            userId = 1L,
            pointTransaction = PointTransaction(
                beforePoint = 100,
                changeAmount = 50,
                afterPoint = 150,
            ),
            detailReason = "체육대회 순위 상금",
        )
        assertChargeLog(
            chargeLog = chargeLogRepository.savedChargeLogs[1],
            userId = 2L,
            pointTransaction = PointTransaction(
                beforePoint = 250,
                changeAmount = 50,
                afterPoint = 300,
            ),
            detailReason = "체육대회 순위 상금",
        )
    }

    @Test
    @DisplayName("대상 사용자 중 포인트 지갑이 없는 사용자가 있으면 아무 지갑도 저장하지 않는다")
    fun `charge fails before saving when any wallet does not exist`() {
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 100)),
        )
        val chargeLogRepository = FakeChargeLogRepository()
        val useCase = BulkChargeWalletUseCase(walletRepository, chargeLogRepository)

        assertFailsWith<WalletNotFoundException> {
            useCase.charge(
                BulkChargeWalletRequest(
                    userIds = listOf(1L, 2L),
                    amount = 50,
                    reason = "이벤트 지급",
                ),
            )
        }

        assertEquals(emptyList(), walletRepository.savedWallets)
        assertEquals(emptyList(), chargeLogRepository.savedChargeLogs)
    }

    private fun assertChargeLog(
        chargeLog: ChargeLog,
        userId: Long,
        pointTransaction: PointTransaction,
        detailReason: String,
    ) {
        assertEquals(userId, chargeLog.userId)
        assertEquals(pointTransaction, chargeLog.pointTransaction)
        assertEquals(ChargeReason.REWARD, chargeLog.chargeReason)
        assertEquals(detailReason, chargeLog.detailReason)
    }

    private class FakeChargeLogRepository : ChargeLogRepository {
        val savedChargeLogs = mutableListOf<ChargeLog>()

        override fun findByPaymentId(paymentId: Long): ChargeLog? = null

        override fun save(chargeLog: ChargeLog): ChargeLog {
            savedChargeLogs += chargeLog
            return chargeLog
        }

        override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> {
            savedChargeLogs += chargeLogs
            return chargeLogs
        }
    }
}
