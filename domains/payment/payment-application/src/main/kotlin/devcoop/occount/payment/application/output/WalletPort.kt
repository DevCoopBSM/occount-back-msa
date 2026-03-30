package devcoop.occount.payment.application.output

interface WalletPort {
    fun getBalance(userId: Long): Int
    fun deduct(userId: Long, amount: Int): Int
}
