package devcoop.occount.payment.application.output

import devcoop.occount.payment.domain.wallet.ChargeLog

interface ChargeLogRepository {
    fun findByPaymentId(paymentId: Long): ChargeLog?
    fun save(chargeLog: ChargeLog): ChargeLog
    fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog>
}
