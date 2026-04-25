package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.ItemStockPayload
import devcoop.occount.core.common.event.PaymentCompletedEvent
import devcoop.occount.core.common.event.PaymentFailedEvent
import devcoop.occount.core.common.event.OrderPaymentPayload
import devcoop.occount.core.common.event.OrderPaymentRequestedEvent
import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.application.dto.response.VanResult
import devcoop.occount.payment.application.exception.PaymentCancelledException
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.OrderPaymentCancellationRequestResult
import devcoop.occount.payment.application.output.OrderPaymentExecutionRepository
import devcoop.occount.payment.application.output.OrderPaymentExecutionStartResult
import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.query.wallet.GetWalletPointQueryService
import devcoop.occount.payment.application.shared.PaymentFacade
import devcoop.occount.payment.application.usecase.wallet.deduct.DeductWalletUseCase
import devcoop.occount.payment.domain.payment.CardType
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.PaymentType
import devcoop.occount.payment.domain.wallet.Wallet
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExecuteVanPaymentUseCaseTest {
    @Test
    fun `execute skips duplicate request`() {
        val executionRepository = FakeOrderPaymentExecutionRepository(startResult = OrderPaymentExecutionStartResult.DUPLICATE)
        val cardPaymentPort = FakeCardPaymentPort()
        val paymentFacade = paymentFacade(cardPaymentPort)
        val eventPublisher = FakeEventPublisher()
        val useCase = ExecuteVanPaymentUseCase(paymentFacade, executionRepository, eventPublisher)

        useCase.execute(requestedEvent())

        assertEquals(0, cardPaymentPort.approvedAmounts.size)
        assertEquals(0, eventPublisher.published.size)
    }

    @Test
    fun `execute publishes failed when request was cancelled before start`() {
        val executionRepository = FakeOrderPaymentExecutionRepository(startResult = OrderPaymentExecutionStartResult.CANCELLED_BEFORE_START)
        val cardPaymentPort = FakeCardPaymentPort()
        val paymentFacade = paymentFacade(cardPaymentPort)
        val eventPublisher = FakeEventPublisher()
        val useCase = ExecuteVanPaymentUseCase(paymentFacade, executionRepository, eventPublisher)

        useCase.execute(requestedEvent())

        assertEquals(1L, executionRepository.cancelledOrderId)
        assertEquals(0, cardPaymentPort.approvedAmounts.size)
        assertIs<PaymentFailedEvent>(eventPublisher.published.single())
    }

    @Test
    fun `execute marks completed and publishes completed event on success`() {
        val executionRepository = FakeOrderPaymentExecutionRepository()
        val cardPaymentPort = FakeCardPaymentPort()
        val paymentFacade = paymentFacade(cardPaymentPort)
        val eventPublisher = FakeEventPublisher()
        val useCase = ExecuteVanPaymentUseCase(paymentFacade, executionRepository, eventPublisher)

        useCase.execute(requestedEvent())

        assertEquals(1L, executionRepository.completedOrderId)
        assertEquals("1", cardPaymentPort.lastPaymentKey)
        assertIs<PaymentCompletedEvent>(eventPublisher.published.single())
    }

    @Test
    fun `execute marks cancelled and publishes failed event when payment is cancelled`() {
        val executionRepository = FakeOrderPaymentExecutionRepository()
        val cardPaymentPort = FakeCardPaymentPort(error = PaymentCancelledException())
        val paymentFacade = paymentFacade(cardPaymentPort)
        val eventPublisher = FakeEventPublisher()
        val useCase = ExecuteVanPaymentUseCase(paymentFacade, executionRepository, eventPublisher)

        useCase.execute(requestedEvent())

        assertEquals(1L, executionRepository.cancelledOrderId)
        assertIs<PaymentFailedEvent>(eventPublisher.published.single())
    }

    private fun requestedEvent(): OrderPaymentRequestedEvent {
        return OrderPaymentRequestedEvent(
            orderId = 1L,
            kioskId = "kiosk-1",
            userId = null,
            payment = OrderPaymentPayload(totalAmount = 2000),
            items = listOf(
                ItemStockPayload(
                    itemId = 101L,
                    itemName = "Americano",
                    itemPrice = 2000,
                    quantity = 1,
                    totalPrice = 2000,
                ),
            ),
        )
    }

    private fun paymentFacade(cardPaymentPort: FakeCardPaymentPort): PaymentFacade {
        return PaymentFacade(
            payWithPointsUseCase = devcoop.occount.payment.application.usecase.payment.PayWithPointsUseCase(
                deductWalletUseCase = DeductWalletUseCase(FakeWalletRepository()),
                paymentLogRepository = FakePaymentLogRepository(),
            ),
            mixedPaymentUseCase = MixedPaymentUseCase(
                getWalletPointQueryService = GetWalletPointQueryService(FakeWalletRepository()),
                deductWalletUseCase = DeductWalletUseCase(FakeWalletRepository()),
                cardPaymentPort = cardPaymentPort,
                paymentLogRepository = FakePaymentLogRepository(),
            ),
            cardOnlyPaymentUseCase = CardOnlyPaymentUseCase(
                cardPaymentPort = cardPaymentPort,
                paymentLogRepository = FakePaymentLogRepository(),
            ),
            getWalletPointQueryService = GetWalletPointQueryService(FakeWalletRepository()),
        )
    }

    private class FakeOrderPaymentExecutionRepository(
        private val startResult: OrderPaymentExecutionStartResult = OrderPaymentExecutionStartResult.STARTED,
    ) : OrderPaymentExecutionRepository {
        var completedOrderId: Long? = null
        var cancelledOrderId: Long? = null

        override fun startProcessing(orderId: Long): OrderPaymentExecutionStartResult = startResult
        override fun requestCancellation(orderId: Long): OrderPaymentCancellationRequestResult = OrderPaymentCancellationRequestResult.NO_ACTIVE_PAYMENT
        override fun isCancellationRequested(orderId: Long): Boolean = false
        override fun markCompleted(orderId: Long) { completedOrderId = orderId }
        override fun markFailed(orderId: Long) = Unit
        override fun markCancelled(orderId: Long) { cancelledOrderId = orderId }
    }

    private class FakeEventPublisher : EventPublisher {
        val published = mutableListOf<Any>()

        override fun publish(topic: String, key: String, eventType: String, payload: Any) {
            published += payload
        }
    }

    private class FakeCardPaymentPort(
        private val error: Exception? = null,
    ) : CardPaymentPort {
        val approvedAmounts = mutableListOf<Int>()
        var lastPaymentKey: String? = null

        override fun approve(amount: Int, items: List<ItemCommand>, kioskId: String, paymentKey: String?): VanResult {
            error?.let { throw it }
            approvedAmounts += amount
            lastPaymentKey = paymentKey
            return VanResult(
                success = true,
                message = "ok",
                errorCode = null,
                transaction = TransactionResult(
                    messageNumber = null,
                    typeCode = null,
                    cardNumber = "1234",
                    amount = amount,
                    installmentMonths = 0,
                    cancelType = null,
                    approvalNumber = "APPROVAL-$amount",
                    approvalDate = "20260312",
                    approvalTime = "101010",
                    transactionId = "TX-$amount",
                    terminalId = "TERM-1",
                    merchantNumber = "MERCHANT-1",
                    rejectCode = null,
                    rejectMessage = null,
                ),
                card = CardResult(
                    acquirerCode = "ACQ",
                    acquirerName = "Acquirer",
                    issuerCode = "ISS",
                    issuerName = "Issuer",
                    cardType = CardType.CREDIT,
                    cardCategory = "PERSONAL",
                    cardName = "Test Card",
                    cardBrand = "VISA",
                ),
                additional = null,
                rawResponse = null,
            )
        }

        override fun refund(transactionId: String?, approvalNumber: String?, approvalDate: String, amount: Int, kioskId: String): VanResult {
            error("not used in this test")
        }

        override fun requestPendingApprovalCancellation(paymentKey: String, kioskId: String) = Unit
    }

    private class FakePaymentLogRepository : PaymentLogRepository {
        override fun findById(paymentId: Long): PaymentLog? = null
        override fun findByUserId(userId: Long): List<PaymentLog> = emptyList()
        override fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentLog> = emptyList()
        override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> = emptyList()
        override fun save(paymentLog: PaymentLog): PaymentLog = paymentLog
        override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> = paymentLogs
    }

    private class FakeWalletRepository : devcoop.occount.payment.application.output.WalletRepository {
        override fun findByUserId(userId: Long): Wallet? = Wallet(userId = userId, point = 0)
        override fun save(wallet: Wallet): Wallet = wallet
    }
}
