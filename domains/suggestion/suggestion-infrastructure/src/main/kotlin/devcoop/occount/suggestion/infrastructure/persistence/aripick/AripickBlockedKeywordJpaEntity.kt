package devcoop.occount.suggestion.infrastructure.persistence.aripick

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "aripick_blocked_keyword")
class AripickBlockedKeywordJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:Column(name = "keyword_id")
    private var keywordId: Long = 0L,
    @field:Column(name = "keyword", nullable = false, unique = true)
    private var keyword: String = "",
    @field:Column(name = "registered_date", nullable = false)
    private var registeredDate: LocalDate = LocalDate.now(),
) {
    fun getKeywordId() = keywordId
    fun getKeyword() = keyword
    fun getRegisteredDate() = registeredDate
}
