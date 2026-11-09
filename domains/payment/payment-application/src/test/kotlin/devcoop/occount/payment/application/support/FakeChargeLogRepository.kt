package devcoop.occount.payment.application.support

import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.domain.wallet.ChargeLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class FakeChargeLogRepository(
    initialLogs: List<ChargeLog> = emptyList(),
    private val saveException: RuntimeException? = null,
) : ChargeLogRepository {
    val savedLogs = mutableListOf<ChargeLog>().apply { addAll(initialLogs) }
    private var nextId = (savedLogs.mapNotNull { it.chargeId.takeIf { id -> id != 0L } }.maxOrNull() ?: 0L) + 1

    override fun findByPaymentId(paymentId: Long): ChargeLog? =
        savedLogs.firstOrNull { it.paymentId == paymentId }

    override fun findAll(pageable: Pageable): Page<ChargeLog> = paginate(savedLogs.toList(), pageable)

    override fun findByUserId(userId: Long, pageable: Pageable): Page<ChargeLog> =
        paginate(savedLogs.filter { it.userId == userId }, pageable)

    private fun paginate(source: List<ChargeLog>, pageable: Pageable): Page<ChargeLog> {
        val sorted = applySort(source, pageable.sort)
        val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(sorted.size)
        val to = (from + pageable.pageSize).coerceAtMost(sorted.size)
        return PageImpl(sorted.subList(from, to), pageable, sorted.size.toLong())
    }

    private fun applySort(logs: List<ChargeLog>, sort: Sort): List<ChargeLog> {
        val order = sort.firstOrNull() ?: return logs
        val comparator: Comparator<ChargeLog> = when (order.property) {
            "chargeDate" -> compareBy { it.chargeDate }
            else -> compareBy { it.chargeId }
        }
        return logs.sortedWith(if (order.isAscending) comparator else comparator.reversed())
    }

    override fun save(chargeLog: ChargeLog): ChargeLog {
        saveException?.let { throw it }
        val persisted = if (chargeLog.chargeId == 0L) chargeLog.copy(chargeId = nextId++) else chargeLog
        savedLogs += persisted
        return persisted
    }

    override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> =
        chargeLogs.map { save(it) }
}
