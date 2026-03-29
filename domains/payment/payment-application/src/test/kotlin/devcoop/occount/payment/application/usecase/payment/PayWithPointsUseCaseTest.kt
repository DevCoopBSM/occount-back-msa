package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.payment.application.output.WalletPort
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentItem
import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.PaymentLogRepository
import devcoop.occount.payment.domain.exception.InsufficientPointsException
import devcoop.occount.payment.domain.type.PaymentType
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PayWithPointsUseCaseTest {
    @Test
    fun `payment uses points only and stores payment history`() {
        val paymentLogRepository = FakePaymentLogRepository()
        val useCase = PayWithPointsUseCase(
            pointWalletPort = FakeWalletPort(balance = 120),
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
        val useCase = PayWithPointsUseCase(
            pointWalletPort = FakeWalletPort(balance = 30),
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

    private class FakeWalletPort(balance: Int) : WalletPort {
        private var currentBalance = balance

        override fun getBalance(userId: Long): Int = currentBalance

        override fun deduct(userId: Long, amount: Int): Int {
            currentBalance -= amount
            return currentBalance
        }
    }

    private class FakePaymentLogRepository : PaymentLogRepository {
        val saved = mutableListOf<PaymentLog>()

        override fun findByUserId(userId: Long): List<PaymentLog> = saved.filter { it.getUserId() == userId }
        override fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentLog> = saved
        override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> = saved.filter { it.getPaymentType() == paymentType }
        override fun save(paymentLog: PaymentLog): PaymentLog { saved += paymentLog; return paymentLog }
        override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> { saved += paymentLogs; return paymentLogs }
    }
}
