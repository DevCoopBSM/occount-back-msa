package devcoop.occount.member.domain.contribution

data class ContributionAccount(
    val userId: Long,
    val balance: Int = 0,
) {
    fun deposit(amount: Int): ContributionAccount {
        if (amount <= 0) {
            throw InvalidContributionDepositAmountException()
        }
        return copy(balance = balance + amount)
    }
}
