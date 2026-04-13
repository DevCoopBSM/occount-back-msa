package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompensatedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.PgResult
import devcoop.occount.payment.application.support.FakeWalletRepository
import devcoop.occount.payment.application.usecase.wallet.charge.ChargeWalletUseCase
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.PaymentType
import devcoop.occount.payment.domain.payment.RefundState
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.Wallet
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CompensateOrderPaymentUseCaseTest {
    @Test
    fun `compensation restores used points and publishes compensated event`() {
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 0)),
        )
        val paymentLogRepository = FakePaymentLogRepository(
            initialLogs = mutableListOf(
                PaymentLog(
                    paymentId = 1L,
                    userId = 1L,
                    paymentType = PaymentType.POINT,
                    totalAmount = 1000,
                ),
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val useCase = CompensateOrderPaymentUseCase(
            paymentLogRepository = paymentLogRepository,
            chargeWalletUseCase = ChargeWalletUseCase(walletRepository, FakeChargeLogRepository()),
            cardPaymentPort = FakeCardPaymentPort(),
            eventPublisher = eventPublisher,
        )

        useCase.compensate(
            OrderPaymentCompensationRequestedEvent(
                orderId = "order-1",
                userId = 1L,
                paymentLogId = 1L,
                pointsUsed = 1000,
                cardAmount = 0,
            ),
        )

        assertEquals(1000, walletRepository.findByUserId(1L)!!.point)
        assertEquals(RefundState.COMPLETED, paymentLogRepository.findById(1L)!!.getRefundState())
        assertIs<OrderPaymentCompensatedEvent>(eventPublisher.published.single())
    }

    private class FakePaymentLogRepository(
        initialLogs: MutableList<PaymentLog> = mutableListOf(),
    ) : PaymentLogRepository {
        private val saved = initialLogs

        override fun findById(paymentId: Long): PaymentLog? = saved.firstOrNull { it.getPaymentId() == paymentId }
        override fun findByUserId(userId: Long): List<PaymentLog> = saved.filter { it.getUserId() == userId }
        override fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentLog> = saved
        override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> = saved.filter { it.getPaymentType() == paymentType }
        override fun save(paymentLog: PaymentLog): PaymentLog {
            saved.removeIf { it.getPaymentId() == paymentLog.getPaymentId() }
            saved += paymentLog
            return paymentLog
        }

        override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> {
            paymentLogs.forEach(::save)
            return paymentLogs
        }
    }

    private class FakeChargeLogRepository : ChargeLogRepository {
        override fun save(chargeLog: ChargeLog): ChargeLog = chargeLog
        override fun findByUserId(userId: Long): List<ChargeLog> = emptyList()
        override fun findByPaymentId(paymentId: Long): ChargeLog? = null
        override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> = chargeLogs
    }

    private class FakeCardPaymentPort : CardPaymentPort {
        override fun approve(amount: Int, items: List<ItemCommand>): PgResult {
            throw UnsupportedOperationException()
        }

        override fun cancel(transactionId: String?, approvalNumber: String?, amount: Int): PgResult {
            return PgResult(true, "ok", null, null, null, null, null)
        }
    }

    private class FakeEventPublisher : EventPublisher {
        val published = mutableListOf<Any>()

        override fun publish(topic: String, key: String, eventType: String, payload: Any) {
            published += payload
        }
    }
}
