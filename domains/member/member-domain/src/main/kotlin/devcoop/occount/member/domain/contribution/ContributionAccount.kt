package devcoop.occount.member.domain.contribution

data class ContributionAccount(
    val userId: Long,
    val balance: Int = 0,
    val version: Long? = null,
) {
    fun deposit(amount: Int): ContributionAccount {
        if (amount <= 0) {
            throw InvalidContributionDepositAmountException()
        }
        return copy(balance = balance + amount)
    }

    fun withdraw(amount: Int): ContributionAccount {
        if (amount <= 0) {
            throw InvalidContributionWithdrawalAmountException()
        }
        if (balance < amount) {
            throw InsufficientContributionBalanceException()
        }
        return copy(balance = balance - amount)
    }
}
