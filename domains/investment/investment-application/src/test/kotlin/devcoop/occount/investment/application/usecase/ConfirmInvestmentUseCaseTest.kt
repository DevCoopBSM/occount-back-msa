package devcoop.occount.investment.application.usecase

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.investment.application.event.InvestmentRegisteredEvent
import devcoop.occount.investment.application.exception.InvestmentAmountMismatchException
import devcoop.occount.investment.application.exception.PendingInvestmentNotFoundException
import devcoop.occount.investment.application.support.FakeEventPublisher
import devcoop.occount.investment.application.output.InvestmentRepository
import devcoop.occount.investment.application.support.FakeInvestmentRepository
import devcoop.occount.investment.application.usecase.confirm.ConfirmInvestmentUseCase
import devcoop.occount.investment.domain.investment.Investment
import devcoop.occount.investment.domain.investment.InvestmentStatus
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConfirmInvestmentUseCaseTest {
    @Test
    fun `confirm transitions pending to confirmed and publishes registered event`() {
        val pending = Investment.pending(userId = 7L, paymentId = "I-abc", amount = 50_000).copy(investmentId = 10L)
        val repository = FakeInvestmentRepository(listOf(pending))
        val publisher = FakeEventPublisher()
        val useCase = ConfirmInvestmentUseCase(repository, publisher)

        useCase.confirm(paymentId = "I-abc", paidAmount = 50_000)

        assertEquals(InvestmentStatus.CONFIRMED, repository.findByPaymentId("I-abc")!!.status)
        val published = publisher.published.single()
        assertEquals(DomainTopics.INVESTMENT_REGISTERED, published.topic)
        assertEquals(DomainEventTypes.INVESTMENT_REGISTERED, published.eventType)
        assertEquals("7", published.key)
        val payload = assertIs<InvestmentRegisteredEvent>(published.payload)
        assertEquals(7L, payload.userId)
        assertEquals(50_000, payload.amount)
    }

    @Test
    fun `confirm is idempotent when already confirmed`() {
        val confirmed = Investment.pending(7L, "I-abc", 50_000).copy(investmentId = 10L).confirm()
        val repository = FakeInvestmentRepository(listOf(confirmed))
        val publisher = FakeEventPublisher()
        val useCase = ConfirmInvestmentUseCase(repository, publisher)

        useCase.confirm(paymentId = "I-abc", paidAmount = 50_000)

        assertTrue(publisher.published.isEmpty())
    }

    @Test
    fun `confirm rejects when paid amount does not match`() {
        val pending = Investment.pending(7L, "I-abc", 50_000).copy(investmentId = 10L)
        val repository = FakeInvestmentRepository(listOf(pending))
        val useCase = ConfirmInvestmentUseCase(repository, FakeEventPublisher())

        assertThrows<InvestmentAmountMismatchException> {
            useCase.confirm(paymentId = "I-abc", paidAmount = 30_000)
        }
        assertEquals(InvestmentStatus.PENDING, repository.findByPaymentId("I-abc")!!.status)
    }

    @Test
    fun `confirm throws when no pending investment exists for paymentId`() {
        val useCase = ConfirmInvestmentUseCase(FakeInvestmentRepository(), FakeEventPublisher())

        assertThrows<PendingInvestmentNotFoundException> {
            useCase.confirm(paymentId = "I-missing", paidAmount = 1_000)
        }
    }

    @Test
    fun `confirm does not publish when it loses the atomic transition race`() {
        // findByPaymentId 는 PENDING 을 돌려주지만 confirmIfPending 이 false(다른 트랜잭션이 먼저 확정)를 반환하는 상황.
        val publisher = FakeEventPublisher()
        val racingRepository = object : InvestmentRepository {
            val pending = Investment.pending(7L, "I-abc", 50_000).copy(investmentId = 10L)
            override fun findByPaymentId(paymentId: String) = pending
            override fun findById(investmentId: Long) = null
            override fun findByUserId(userId: Long, pageable: org.springframework.data.domain.Pageable) =
                org.springframework.data.domain.Page.empty<Investment>(pageable)
            override fun findAll(pageable: org.springframework.data.domain.Pageable) =
                org.springframework.data.domain.Page.empty<Investment>(pageable)
            override fun save(investment: Investment) = investment
            override fun confirmIfPending(paymentId: String, confirmedAt: java.time.LocalDateTime) = false
        }
        val useCase = ConfirmInvestmentUseCase(racingRepository, publisher)

        useCase.confirm(paymentId = "I-abc", paidAmount = 50_000)

        assertTrue(publisher.published.isEmpty())
    }
}
