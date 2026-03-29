package devcoop.occount.point.domain

interface ChargeLogRepository {
    fun findByUserId(userId: Long): List<ChargeLog>
    fun findByPaymentId(paymentId: Long): ChargeLog?
    fun save(chargeLog: ChargeLog): ChargeLog
    fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog>
}
