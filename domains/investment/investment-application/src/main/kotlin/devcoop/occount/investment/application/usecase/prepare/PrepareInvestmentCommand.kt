package devcoop.occount.investment.application.usecase.prepare

data class PrepareInvestmentCommand(
    val userId: Long,
    val amount: Int,
)
