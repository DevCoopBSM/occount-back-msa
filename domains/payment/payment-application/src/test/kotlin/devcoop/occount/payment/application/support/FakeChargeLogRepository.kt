package devcoop.occount.payment.application.support

import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.domain.wallet.ChargeLog

class FakeChargeLogRepository(
    private val saveException: RuntimeException? = null,
) : ChargeLogRepository {
    val savedLogs = mutableListOf<ChargeLog>()
    private var nextId = 1L

    override fun findByPaymentId(paymentId: Long): ChargeLog? =
        savedLogs.firstOrNull { it.paymentId == paymentId }

    override fun save(chargeLog: ChargeLog): ChargeLog {
        saveException?.let { throw it }
        val persisted = if (chargeLog.chargeId == 0L) chargeLog.copy(chargeId = nextId++) else chargeLog
        savedLogs += persisted
        return persisted
    }

    override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> =
        chargeLogs.map { save(it) }
}
