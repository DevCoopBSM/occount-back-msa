package devcoop.occount.investment.application.usecase

import devcoop.occount.investment.application.support.FakeInvestmentRepository
import devcoop.occount.investment.application.usecase.prepare.PrepareInvestmentCommand
import devcoop.occount.investment.application.usecase.prepare.PrepareInvestmentUseCase
import devcoop.occount.investment.domain.investment.InvestmentStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrepareInvestmentUseCaseTest {
    @Test
    fun `prepare saves a PENDING investment and returns an investment paymentId`() {
        val repository = FakeInvestmentRepository()
        val useCase = PrepareInvestmentUseCase(repository)

        val result = useCase.prepare(PrepareInvestmentCommand(userId = 1L, amount = 50_000))

        assertTrue(result.paymentId.startsWith(PrepareInvestmentUseCase.INVESTMENT_PAYMENT_PREFIX))
        val saved = repository.saved.single()
        assertEquals(1L, saved.userId)
        assertEquals(50_000, saved.amount)
        assertEquals(InvestmentStatus.PENDING, saved.status)
        assertEquals(result.paymentId, saved.paymentId)
    }
}
