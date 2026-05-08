package devcoop.occount.inquiry.infrastructure.persistence

import devcoop.occount.inquiry.domain.inquiry.InquiryCategory
import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "inquiries")
class InquiryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @field:Column(nullable = false)
    val userId: Long,

    @field:Column(nullable = false, length = 100)
    val title: String,

    @field:Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    val category: InquiryCategory,

    @Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    val status: InquiryStatus = InquiryStatus.RECEIVED,

    @field:Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @field:Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
