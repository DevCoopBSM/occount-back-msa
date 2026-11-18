package devcoop.occount.member.domain.contribution

data class ContributionAccount(
    val userId: Long,
    val balance: Long = 0,
    val version: Long? = null,
) {
    fun deposit(amount: Long): ContributionAccount {
        if (amount <= 0) {
            throw InvalidContributionDepositAmountException()
        }
        val updatedBalance = try {
            Math.addExact(balance, amount)
        } catch (_: ArithmeticException) {
            throw ContributionBalanceOverflowException()
        }
        return copy(balance = updatedBalance)
    }

    fun withdraw(amount: Long): ContributionAccount {
        if (amount <= 0) {
            throw InvalidContributionWithdrawalAmountException()
        }
        if (balance < amount) {
            throw InsufficientContributionBalanceException()
        }
        return copy(balance = balance - amount)
    }
}
