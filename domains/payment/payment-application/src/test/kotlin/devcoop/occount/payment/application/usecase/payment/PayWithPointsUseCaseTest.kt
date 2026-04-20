package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentItem
import devcoop.occount.payment.application.support.FakeWalletRepository
import devcoop.occount.payment.application.usecase.wallet.deduct.DeductWalletUseCase
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.wallet.Wallet
import devcoop.occount.payment.domain.wallet.InsufficientPointsException
import devcoop.occount.payment.domain.payment.PaymentType
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PayWithPointsUseCaseTest {
    @Test
    fun `payment uses points only and stores payment history`() {
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 120)),
        )
        val paymentLogRepository = FakePaymentLogRepository()
        val useCase = PayWithPointsUseCase(
            deductWalletUseCase = DeductWalletUseCase(walletRepository),
            paymentLogRepository = paymentLogRepository,
        )

        val response = useCase.execute(
            userId = 1L,
            details = paymentDetails(totalAmount = 80),
        )

        assertEquals(PaymentType.POINT, response.type)
        assertEquals(80, response.pointsUsed)
        assertNull(response.cardAmount)
        assertEquals(40, response.remainingPoints)
        assertEquals(1, paymentLogRepository.saved.size)
    }

    @Test
    fun `payment fails when points are insufficient`() {
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 30)),
        )
        val useCase = PayWithPointsUseCase(
            deductWalletUseCase = DeductWalletUseCase(walletRepository),
            paymentLogRepository = FakePaymentLogRepository(),
        )

        assertFailsWith<InsufficientPointsException> {
            useCase.execute(userId = 1L, details = paymentDetails(totalAmount = 80))
        }
    }

    private fun paymentDetails(totalAmount: Int): PaymentDetails {
        return PaymentDetails(
            items = listOf(
                PaymentItem(
                    itemId = "ITEM-1",
                    itemName = "Coffee",
                    itemPrice = totalAmount,
                    quantity = 1,
                    totalPrice = totalAmount,
                ),
            ),
            totalAmount = totalAmount,
        )
    }

    private class FakePaymentLogRepository : PaymentLogRepository {
        val saved = mutableListOf<PaymentLog>()

        override fun findById(paymentId: Long): PaymentLog? = saved.firstOrNull { it.getPaymentId() == paymentId }
        override fun findByUserId(userId: Long): List<PaymentLog> = saved.filter { it.getUserId() == userId }
        override fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentLog> = saved
        override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> = saved.filter { it.getPaymentType() == paymentType }
        override fun save(paymentLog: PaymentLog): PaymentLog { saved += paymentLog; return paymentLog }
        override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> { saved += paymentLogs; return paymentLogs }
    }
}
