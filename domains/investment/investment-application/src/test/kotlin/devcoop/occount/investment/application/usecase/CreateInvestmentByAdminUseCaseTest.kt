package devcoop.occount.investment.application.usecase

import devcoop.occount.investment.application.event.InvestmentRegisteredEvent
import devcoop.occount.investment.application.support.FakeEventPublisher
import devcoop.occount.investment.application.support.FakeInvestmentRepository
import devcoop.occount.investment.application.usecase.admin.AdminCreateInvestmentCommand
import devcoop.occount.investment.application.usecase.admin.CreateInvestmentByAdminUseCase
import devcoop.occount.investment.domain.investment.Investment
import devcoop.occount.investment.domain.investment.InvestmentStatus
import devcoop.occount.investment.domain.investment.InvestmentType
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreateInvestmentByAdminUseCaseTest {
    @Test
    fun `creates a confirmed investment and publishes registered event`() {
        val repository = FakeInvestmentRepository()
        val publisher = FakeEventPublisher()
        val useCase = CreateInvestmentByAdminUseCase(repository, publisher)

        useCase.create(
            AdminCreateInvestmentCommand(
                userId = 42L,
                amount = 30_000,
                type = InvestmentType.DEPOSIT.name,
                depositDate = LocalDateTime.of(2026, 6, 16, 9, 0),
                conversionDate = null,
                confirmMethod = Investment.MANUAL_CONFIRM_METHOD,
            ),
        )

        val saved = repository.saved.single()
        assertEquals(InvestmentStatus.CONFIRMED, saved.status)
        assertEquals(42L, saved.userId)
        assertTrue(saved.paymentId.startsWith(CreateInvestmentByAdminUseCase.ADMIN_PAYMENT_PREFIX))

        val payload = assertIs<InvestmentRegisteredEvent>(publisher.published.single().payload)
        assertEquals(42L, payload.userId)
        assertEquals(30_000, payload.amount)
    }
}
