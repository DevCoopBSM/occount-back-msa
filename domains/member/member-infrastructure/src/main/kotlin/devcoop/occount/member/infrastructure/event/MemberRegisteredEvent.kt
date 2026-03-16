package devcoop.occount.member.infrastructure.event

data class MemberRegisteredEvent(
    val userId: Long,
    val email: String,
)
