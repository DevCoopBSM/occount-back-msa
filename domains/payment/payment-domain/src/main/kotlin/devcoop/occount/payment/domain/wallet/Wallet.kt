package devcoop.occount.payment.domain.wallet

data class Wallet(
    val userId: Long,
    val point: Int = 0,
    val version: Long = 0L,
) {
    fun charge(amount: Int): Wallet {
        if (amount <= 0) {
            throw InvalidChargeAmountException()
        }
        return copy(point = point + amount)
    }

    fun deduct(amount: Int): Wallet {
        if (amount <= 0) {
            throw InvalidDeductAmountException()
        }
        if (point < amount) {
            throw InsufficientPointsException()
        }

        return copy(point = point - amount)
    }
}
