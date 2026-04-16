package devcoop.occount.payment.application.dto.request

data class VanCancelCommand(
    val approvalNumber: String,
    val approvalDate: String,
    val amount: Int
)
