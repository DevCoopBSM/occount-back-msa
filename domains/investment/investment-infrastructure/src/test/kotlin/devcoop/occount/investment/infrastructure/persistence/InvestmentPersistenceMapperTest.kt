package devcoop.occount.investment.infrastructure.persistence

import devcoop.occount.investment.domain.investment.Investment
import devcoop.occount.investment.domain.investment.InvestmentStatus
import devcoop.occount.investment.domain.investment.InvestmentType
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class InvestmentPersistenceMapperTest {
    @Test
    fun `domain to entity to domain preserves fields`() {
        val domain = Investment(
            investmentId = 11L,
            userId = 7L,
            paymentId = "I-abc",
            amount = 50_000,
            type = InvestmentType.DEPOSIT,
            status = InvestmentStatus.CONFIRMED,
            depositDate = LocalDateTime.of(2026, 6, 16, 9, 0),
            conversionDate = null,
            confirmMethod = Investment.AUTO_CONFIRM_METHOD,
        )

        val roundTrip = InvestmentPersistenceMapper.toDomain(InvestmentPersistenceMapper.toEntity(domain))

        assertEquals(domain, roundTrip)
        assertEquals(domain.userId, roundTrip.userId)
        assertEquals(domain.paymentId, roundTrip.paymentId)
        assertEquals(domain.amount, roundTrip.amount)
        assertEquals(domain.status, roundTrip.status)
        assertEquals(domain.depositDate, roundTrip.depositDate)
    }
}
