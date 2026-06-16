package devcoop.occount.investment.application.usecase

import devcoop.occount.investment.application.output.PortOnePayment
import devcoop.occount.investment.application.output.WebhookNotification
import devcoop.occount.investment.application.support.FakeEventPublisher
import devcoop.occount.investment.application.support.FakeInvestmentRepository
import devcoop.occount.investment.application.support.FakePortOnePaymentPort
import devcoop.occount.investment.application.support.FakePortOneWebhookGateway
import devcoop.occount.investment.application.usecase.confirm.ConfirmInvestmentUseCase
import devcoop.occount.investment.application.usecase.confirm.HandlePortOneWebhookUseCase
import devcoop.occount.investment.domain.investment.Investment
import devcoop.occount.investment.domain.investment.InvestmentStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandlePortOneWebhookUseCaseTest {
    private fun useCase(
        notification: WebhookNotification?,
        payments: Map<String, PortOnePayment>,
        repository: FakeInvestmentRepository,
        publisher: FakeEventPublisher = FakeEventPublisher(),
    ): HandlePortOneWebhookUseCase {
        return HandlePortOneWebhookUseCase(
            portOneWebhookGateway = FakePortOneWebhookGateway(notification),
            portOnePaymentPort = FakePortOnePaymentPort(payments),
            confirmInvestmentUseCase = ConfirmInvestmentUseCase(repository, publisher),
        )
    }

    @Test
    fun `confirms investment when webhook is paid`() {
        val pending = Investment.pending(7L, "I-abc", 50_000).copy(investmentId = 1L)
        val repository = FakeInvestmentRepository(listOf(pending))
        val publisher = FakeEventPublisher()
        val useCase = useCase(
            notification = WebhookNotification("Transaction.Paid", "I-abc"),
            payments = mapOf("I-abc" to PortOnePayment(status = "PAID", amount = 50_000)),
            repository = repository,
            publisher = publisher,
        )

        useCase.handle("{}", emptyMap())

        assertEquals(InvestmentStatus.CONFIRMED, repository.findByPaymentId("I-abc")!!.status)
        assertEquals(1, publisher.published.size)
    }

    @Test
    fun `ignores event types other than paid or ready`() {
        val pending = Investment.pending(7L, "I-abc", 50_000).copy(investmentId = 1L)
        val repository = FakeInvestmentRepository(listOf(pending))
        val useCase = useCase(
            notification = WebhookNotification("Transaction.Cancelled", "I-abc"),
            payments = emptyMap(),
            repository = repository,
        )

        useCase.handle("{}", emptyMap())

        assertEquals(InvestmentStatus.PENDING, repository.findByPaymentId("I-abc")!!.status)
    }

    @Test
    fun `ignores non-investment paymentId`() {
        val repository = FakeInvestmentRepository()
        val useCase = useCase(
            notification = WebhookNotification("Transaction.Paid", "A-charge-123"),
            payments = emptyMap(),
            repository = repository,
        )

        // 충전(prefix 미일치) 결제는 PortOne 조회조차 하지 않고 무시한다.
        useCase.handle("{}", emptyMap())

        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `ignores when payment is not yet paid`() {
        val pending = Investment.pending(7L, "I-abc", 50_000).copy(investmentId = 1L)
        val repository = FakeInvestmentRepository(listOf(pending))
        val useCase = useCase(
            notification = WebhookNotification("Transaction.Ready", "I-abc"),
            payments = mapOf("I-abc" to PortOnePayment(status = "READY", amount = 50_000)),
            repository = repository,
        )

        useCase.handle("{}", emptyMap())

        assertEquals(InvestmentStatus.PENDING, repository.findByPaymentId("I-abc")!!.status)
    }

    @Test
    fun `does nothing when gateway returns null notification`() {
        val repository = FakeInvestmentRepository()
        val useCase = useCase(notification = null, payments = emptyMap(), repository = repository)

        useCase.handle("{}", emptyMap())

        assertTrue(repository.saved.isEmpty())
    }
}
