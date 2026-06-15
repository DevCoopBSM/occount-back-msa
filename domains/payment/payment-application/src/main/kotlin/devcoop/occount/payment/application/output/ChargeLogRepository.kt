package devcoop.occount.payment.application.output

import devcoop.occount.payment.domain.wallet.ChargeLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ChargeLogRepository {
    fun findByPaymentId(paymentId: Long): ChargeLog?
    fun findAll(pageable: Pageable): Page<ChargeLog>
    fun findByUserId(userId: Long, pageable: Pageable): Page<ChargeLog>
    fun save(chargeLog: ChargeLog): ChargeLog
    fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog>
}
