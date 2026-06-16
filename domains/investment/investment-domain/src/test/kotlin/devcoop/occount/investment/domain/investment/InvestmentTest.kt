package devcoop.occount.investment.domain.investment

import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InvestmentTest {
    @Test
    fun `pending creates a PENDING deposit with no deposit date`() {
        val investment = Investment.pending(userId = 1L, paymentId = "I-abc", amount = 50_000)

        assertEquals(InvestmentStatus.PENDING, investment.status)
        assertEquals(InvestmentType.DEPOSIT, investment.type)
        assertEquals(Investment.AUTO_CONFIRM_METHOD, investment.confirmMethod)
        assertNull(investment.depositDate)
    }

    @Test
    fun `confirm transitions PENDING to CONFIRMED and sets deposit date`() {
        val confirmedAt = LocalDateTime.of(2026, 6, 16, 10, 0)
        val confirmed = Investment.pending(1L, "I-abc", 50_000).confirm(confirmedAt)

        assertEquals(InvestmentStatus.CONFIRMED, confirmed.status)
        assertEquals(confirmedAt, confirmed.depositDate)
    }

    @Test
    fun `confirm on already confirmed throws`() {
        val confirmed = Investment.pending(1L, "I-abc", 50_000).confirm()

        assertThrows<InvestmentAlreadyConfirmedException> { confirmed.confirm() }
    }

    @Test
    fun `non-positive amount is rejected`() {
        assertThrows<InvalidInvestmentAmountException> { Investment.pending(1L, "I-abc", 0) }
        assertThrows<InvalidInvestmentAmountException> { Investment.pending(1L, "I-abc", -1) }
    }

    @Test
    fun `confirmedByAdmin creates a CONFIRMED investment directly`() {
        val depositDate = LocalDateTime.of(2026, 6, 16, 9, 0)
        val investment = Investment.confirmedByAdmin(
            userId = 7L,
            paymentId = "ADMIN-xyz",
            amount = 30_000,
            type = InvestmentType.DEPOSIT,
            depositDate = depositDate,
            conversionDate = null,
            confirmMethod = Investment.MANUAL_CONFIRM_METHOD,
        )

        assertEquals(InvestmentStatus.CONFIRMED, investment.status)
        assertEquals(depositDate, investment.depositDate)
        assertEquals(Investment.MANUAL_CONFIRM_METHOD, investment.confirmMethod)
    }
}
