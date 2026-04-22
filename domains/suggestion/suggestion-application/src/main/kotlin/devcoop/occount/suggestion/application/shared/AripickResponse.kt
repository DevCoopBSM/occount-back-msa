package devcoop.occount.suggestion.application.shared

import devcoop.occount.suggestion.domain.aripick.AripickStatus
import java.time.LocalDate

data class AripickResponse(
    val proposalId: Long,
    val name: String,
    val reason: String,
    val proposerId: Long,
    val proposalDate: LocalDate,
    val status: AripickStatus,
    val likeCount: Int,
)
