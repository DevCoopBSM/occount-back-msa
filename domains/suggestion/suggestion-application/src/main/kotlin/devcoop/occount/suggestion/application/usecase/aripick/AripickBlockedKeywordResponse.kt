package devcoop.occount.suggestion.application.usecase.aripick

import java.time.LocalDate

data class AripickBlockedKeywordResponse(
    val keywordId: Long,
    val keyword: String,
    val registeredDate: LocalDate,
)

data class AripickBlockedKeywordListResponse(
    val keywords: List<AripickBlockedKeywordResponse>,
)

data class CreateAripickBlockedKeywordRequest(
    val keyword: String,
)
