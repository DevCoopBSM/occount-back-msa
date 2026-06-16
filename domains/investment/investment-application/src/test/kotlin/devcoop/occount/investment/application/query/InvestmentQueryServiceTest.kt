package devcoop.occount.investment.application.query

import devcoop.occount.investment.application.exception.InvestmentAccessDeniedException
import devcoop.occount.investment.application.exception.InvestmentNotFoundException
import devcoop.occount.investment.application.support.FakeInvestmentRepository
import devcoop.occount.investment.domain.investment.Investment
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class InvestmentQueryServiceTest {
    private fun confirmed(id: Long, userId: Long): Investment =
        Investment.pending(userId, "I-$id", 50_000).copy(investmentId = id).confirm()

    @Test
    fun `getMyInvestments returns only own investments with paging meta`() {
        val repository = FakeInvestmentRepository(
            listOf(
                confirmed(1L, userId = 1L),
                confirmed(2L, userId = 2L),
                confirmed(3L, userId = 1L),
            ),
        )
        val service = GetMyInvestmentsQueryService(repository)

        val response = service.getMyInvestments(userId = 1L, PageRequest.of(0, 10))

        assertEquals(2L, response.totalCount)
        assertEquals(listOf(3L, 1L), response.investments.map { it.investmentId })
    }

    @Test
    fun `getDetail returns the investment for its owner`() {
        val repository = FakeInvestmentRepository(listOf(confirmed(5L, userId = 9L)))
        val service = GetInvestmentDetailQueryService(repository)

        val result = service.getDetail(investmentId = 5L, requesterUserId = 9L)

        assertEquals(5L, result.investmentId)
    }

    @Test
    fun `getDetail denies access for non-owner`() {
        val repository = FakeInvestmentRepository(listOf(confirmed(5L, userId = 9L)))
        val service = GetInvestmentDetailQueryService(repository)

        assertThrows<InvestmentAccessDeniedException> {
            service.getDetail(investmentId = 5L, requesterUserId = 1L)
        }
    }

    @Test
    fun `getDetail throws when investment is missing`() {
        val service = GetInvestmentDetailQueryService(FakeInvestmentRepository())

        assertThrows<InvestmentNotFoundException> {
            service.getDetail(investmentId = 404L, requesterUserId = 1L)
        }
    }

    @Test
    fun `admin findAll returns all investments with internal fields`() {
        val repository = FakeInvestmentRepository(
            listOf(confirmed(1L, userId = 1L), confirmed(2L, userId = 2L)),
        )
        val service = AdminListInvestmentsQueryService(repository)

        val response = service.findAll(PageRequest.of(0, 10))

        assertEquals(2L, response.totalCount)
        assertEquals(setOf(1L, 2L), response.investments.map { it.userId }.toSet())
    }
}
