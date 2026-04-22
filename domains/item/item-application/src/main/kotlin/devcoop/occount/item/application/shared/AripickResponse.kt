package devcoop.occount.item.application.shared

import devcoop.occount.item.domain.aripick.AripickStatus
import java.time.LocalDate

class AripickResponse (
    val proposalId: Long,
    val name: String,
    val reason: String,
    val proposerId: Long,
    val proposalDate: LocalDate,
    val status: AripickStatus,
    val likeCount: Int,
)
