package devcoop.occount.investment.application.support

import devcoop.occount.investment.application.output.InvestmentRepository
import devcoop.occount.investment.domain.investment.Investment
import devcoop.occount.investment.domain.investment.InvestmentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.time.LocalDateTime

class FakeInvestmentRepository(
    initial: List<Investment> = emptyList(),
) : InvestmentRepository {
    val saved = mutableListOf<Investment>().apply { addAll(initial) }
    private var nextId = (saved.mapNotNull { it.investmentId.takeIf { id -> id != 0L } }.maxOrNull() ?: 0L) + 1

    override fun findByPaymentId(paymentId: String): Investment? =
        saved.firstOrNull { it.paymentId == paymentId }

    override fun findById(investmentId: Long): Investment? =
        saved.firstOrNull { it.investmentId == investmentId }

    override fun findByUserId(userId: Long, pageable: Pageable): Page<Investment> =
        paginate(saved.filter { it.userId == userId }, pageable)

    override fun findAll(pageable: Pageable): Page<Investment> = paginate(saved.toList(), pageable)

    override fun save(investment: Investment): Investment {
        val existingIndex = saved.indexOfFirst { it.investmentId != 0L && it.investmentId == investment.investmentId }
        val persisted = if (investment.investmentId == 0L) investment.copy(investmentId = nextId++) else investment
        if (existingIndex >= 0) {
            saved[existingIndex] = persisted
        } else {
            saved += persisted
        }
        return persisted
    }

    override fun confirmIfPending(paymentId: String, confirmedAt: LocalDateTime): Boolean {
        val index = saved.indexOfFirst { it.paymentId == paymentId && it.status == InvestmentStatus.PENDING }
        if (index < 0) return false
        saved[index] = saved[index].copy(status = InvestmentStatus.CONFIRMED, depositDate = confirmedAt)
        return true
    }

    private fun paginate(source: List<Investment>, pageable: Pageable): Page<Investment> {
        val sorted = applySort(source, pageable.sort)
        val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(sorted.size)
        val to = (from + pageable.pageSize).coerceAtMost(sorted.size)
        return PageImpl(sorted.subList(from, to), pageable, sorted.size.toLong())
    }

    private fun applySort(source: List<Investment>, sort: Sort): List<Investment> {
        val order = sort.firstOrNull() ?: return source.sortedByDescending { it.investmentId }
        val comparator: Comparator<Investment> = when (order.property) {
            "depositDate" -> compareBy { it.depositDate }
            else -> compareBy { it.investmentId }
        }
        return source.sortedWith(if (order.isAscending) comparator else comparator.reversed())
    }
}
