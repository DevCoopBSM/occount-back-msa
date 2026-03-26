package devcoop.occount.point.domain

data class Point(
    val userId: Long,
    val balance: Int = 0,
) {
    fun charge(amount: Int): Point {
        validateAmount(amount)
        return copy(balance = balance + amount)
    }

    fun deduct(amount: Int): Point {
        validateAmount(amount)
        if (balance < amount) {
            throw InsufficientPointBalanceException()
        }

        return copy(balance = balance - amount)
    }

    private fun validateAmount(amount: Int) {
        if (amount <= 0) {
            throw InvalidPointAmountException()
        }
    }
}
