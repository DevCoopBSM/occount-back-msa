package devcoop.occount.suggestion.domain.aripick

import java.time.LocalDate

data class AripickBlockedKeyword(
    private val keywordId: Long = 0L,
    private val keyword: String = "",
    private val registeredDate: LocalDate = LocalDate.now(),
) {
    fun getKeywordId() = keywordId
    fun getKeyword() = keyword
    fun getRegisteredDate() = registeredDate
}
