package devcoop.occount.member.application.usecase.pin

data class PinChangeTicketResponse(
    val ticket: String,
    val expiresIn: Long,
)
