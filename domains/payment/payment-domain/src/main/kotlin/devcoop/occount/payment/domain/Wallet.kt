package devcoop.occount.payment.domain

import devcoop.occount.payment.domain.exception.InsufficientPointBalanceException
import devcoop.occount.payment.domain.exception.InvalidPointAmountException

data class Wallet(
    val userId: Long,
    val balance: Int = 0,
) {
    fun charge(amount: Int): Wallet {
        validateAmount(amount)
        return copy(balance = balance + amount)
    }

    fun deduct(amount: Int): Wallet {
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
