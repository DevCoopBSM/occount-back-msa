package devcoop.occount.suggestion.application.shared

import java.time.LocalDate

data class AripickBlockedKeywordResponse(
    val keywordId: Long,
    val keyword: String,
    val registeredDate: LocalDate,
)
