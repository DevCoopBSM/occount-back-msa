package devcoop.occount.point.application.output

interface ChargePaymentPort {
    fun processCharge(userId: Long, amount: Int): Long
}
