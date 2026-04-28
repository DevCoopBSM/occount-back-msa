package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.PaymentCompensatedEvent
import devcoop.occount.core.common.event.PaymentCompensationFailedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.VanResult
import devcoop.occount.payment.application.exception.PaymentFailedException
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.support.FakeWalletRepository
import devcoop.occount.payment.application.usecase.wallet.refund.RefundWalletPointsUseCase
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.PaymentType
import devcoop.occount.payment.domain.payment.RefundState
import devcoop.occount.payment.domain.payment.TransactionInfo
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.ChargeReason
import devcoop.occount.payment.domain.wallet.PointTransaction
import devcoop.occount.payment.domain.wallet.Wallet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CompensateOrderPaymentUseCaseTest {
    @Test
    fun `compensate refunds mixed payment and publishes completed event`() {
        val paymentLogRepository = FakePaymentLogRepository(
            paymentLog(
                paymentId = 10L,
                paymentType = PaymentType.MIXED,
                pointTransaction = PointTransaction(beforePoint = 1000, changeAmount = -500, afterPoint = 500),
            ),
        )
        val chargeLogRepository = FakeChargeLogRepository()
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 500)),
        )
        val cardPaymentPort = FakeCardPaymentPort()
        val eventPublisher = FakeEventPublisher()
        val useCase = CompensateOrderPaymentUseCase(
            paymentLogRepository = paymentLogRepository,
            cardPaymentPort = cardPaymentPort,
            refundWalletPointsUseCase = RefundWalletPointsUseCase(walletRepository, chargeLogRepository),
            eventPublisher = eventPublisher,
        )

        useCase.compensate(
            OrderPaymentCompensationRequestedEvent(
                orderId = 1L,
                kioskId = "kiosk-1",
                userId = 1L,
                paymentLogId = 10L,
                pointsUsed = 500,
                cardAmount = 1500,
            ),
        )

        assertEquals(1, cardPaymentPort.refundRequests.size)
        assertEquals("20260312", cardPaymentPort.refundRequests.single().approvalDate)
        assertEquals("267733358", cardPaymentPort.refundRequests.single().terminalId)
        assertEquals(1, chargeLogRepository.saved.size)
        assertEquals(1000, walletRepository.findByUserId(1L)?.point)
        assertIs<PaymentCompensatedEvent>(eventPublisher.published.single())
        assertEquals(RefundState.COMPLETED, paymentLogRepository.require(10L).getRefundState())
        assertEquals(RefundState.COMPLETED, paymentLogRepository.require(10L).getCardRefundState())
        assertEquals(RefundState.COMPLETED, paymentLogRepository.require(10L).getPointRefundState())
    }

    @Test
    fun `compensate publishes failed event on terminal error`() {
        val paymentLogRepository = FakePaymentLogRepository(
            paymentLog(
                paymentId = 10L,
                paymentType = PaymentType.CARD,
                transactionInfo = null,
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val useCase = CompensateOrderPaymentUseCase(
            paymentLogRepository = paymentLogRepository,
            cardPaymentPort = FakeCardPaymentPort(),
            refundWalletPointsUseCase = RefundWalletPointsUseCase(FakeWalletRepository(), FakeChargeLogRepository()),
            eventPublisher = eventPublisher,
        )

        useCase.compensate(
            OrderPaymentCompensationRequestedEvent(
                orderId = 1L,
                kioskId = "kiosk-1",
                userId = null,
                paymentLogId = 10L,
                pointsUsed = 0,
                cardAmount = 1500,
            ),
        )

        assertIs<PaymentCompensationFailedEvent>(eventPublisher.published.single())
        assertEquals(RefundState.REQUESTED, paymentLogRepository.require(10L).getRefundState())
    }

    @Test
    fun `compensate rethrows retryable refund error`() {
        val paymentLogRepository = FakePaymentLogRepository(
            paymentLog(
                paymentId = 10L,
                paymentType = PaymentType.CARD,
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val useCase = CompensateOrderPaymentUseCase(
            paymentLogRepository = paymentLogRepository,
            cardPaymentPort = FakeCardPaymentPort(refundError = PaymentFailedException()),
            refundWalletPointsUseCase = RefundWalletPointsUseCase(FakeWalletRepository(), FakeChargeLogRepository()),
            eventPublisher = eventPublisher,
        )

        assertFailsWith<PaymentFailedException> {
            useCase.compensate(
                OrderPaymentCompensationRequestedEvent(
                    orderId = 1L,
                    kioskId = "kiosk-1",
                    userId = null,
                    paymentLogId = 10L,
                    pointsUsed = 0,
                    cardAmount = 1500,
                ),
            )
        }

        assertEquals(emptyList(), eventPublisher.published)
        assertEquals(RefundState.REQUESTED, paymentLogRepository.require(10L).getRefundState())
    }

    private fun paymentLog(
        paymentId: Long,
        paymentType: PaymentType,
        pointTransaction: PointTransaction? = null,
        transactionInfo: TransactionInfo? = TransactionInfo(
            transactionId = "TX-1",
            approvalNumber = "APPROVAL-1",
            approvalDate = "20260312",
            terminalId = "267733358",
        ),
    ): PaymentLog {
        return PaymentLog(
            paymentId = paymentId,
            userId = 1L,
            paymentType = paymentType,
            totalAmount = 2000,
            pointTransaction = pointTransaction,
            transactionInfo = transactionInfo,
        )
    }

    private class FakePaymentLogRepository(
        paymentLog: PaymentLog,
    ) : PaymentLogRepository {
        private val paymentLogs = linkedMapOf(paymentLog.getPaymentId() to paymentLog)

        override fun findById(paymentId: Long): PaymentLog? = paymentLogs[paymentId]
        override fun findByUserId(userId: Long): List<PaymentLog> = paymentLogs.values.filter { it.getUserId() == userId }
        override fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: java.time.LocalDateTime, endDate: java.time.LocalDateTime): List<PaymentLog> = paymentLogs.values.toList()
        override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> = paymentLogs.values.filter { it.getPaymentType() == paymentType }
        override fun save(paymentLog: PaymentLog): PaymentLog {
            paymentLogs[paymentLog.getPaymentId()] = paymentLog
            return paymentLog
        }

        override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> {
            paymentLogs.forEach(::save)
            return paymentLogs
        }

        fun require(paymentId: Long): PaymentLog = requireNotNull(paymentLogs[paymentId])
    }

    private class FakeChargeLogRepository : ChargeLogRepository {
        val saved = mutableListOf<ChargeLog>()

        override fun findByPaymentId(paymentId: Long): ChargeLog? = saved.firstOrNull { it.paymentId == paymentId }
        override fun save(chargeLog: ChargeLog): ChargeLog {
            saved += chargeLog
            return chargeLog
        }

        override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> {
            saved += chargeLogs
            return chargeLogs
        }
    }

    private class FakeCardPaymentPort(
        private val refundError: Exception? = null,
    ) : CardPaymentPort {
        val refundRequests = mutableListOf<RefundRequest>()

        override fun approve(amount: Int, items: List<ItemCommand>, kioskId: String, paymentKey: Long?): VanResult {
            error("not used in this test")
        }

        override fun refund(transactionId: String?, approvalNumber: String?, approvalDate: String, terminalId: String?, amount: Int, kioskId: String): VanResult {
            refundError?.let { throw it }
            refundRequests += RefundRequest(transactionId, approvalNumber, approvalDate, terminalId, amount, kioskId)
            return VanResult(
                success = true,
                message = "ok",
                errorCode = null,
                transaction = null,
                card = null,
                additional = null,
                rawResponse = null,
            )
        }

        override fun requestPendingApprovalCancellation(paymentKey: Long, kioskId: String) {
            error("not used in this test")
        }
    }

    private data class RefundRequest(
        val transactionId: String?,
        val approvalNumber: String?,
        val approvalDate: String,
        val terminalId: String?,
        val amount: Int,
        val kioskId: String,
    )

    private class FakeEventPublisher : EventPublisher {
        val published = mutableListOf<Any>()

        override fun publish(topic: String, key: String, eventType: String, payload: Any) {
            published += payload
        }
    }
}
