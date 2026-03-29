package devcoop.occount.payment.application.output

interface PointWalletPort {
    fun getBalance(userId: Long): Int
    fun deduct(userId: Long, amount: Int): Int
}
