package devcoop.occount.inquiry.infrastructure.persistence

import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InquiryJpaRepository : JpaRepository<InquiryJpaEntity, Long> {
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<InquiryJpaEntity>
    fun findAllByStatus(status: InquiryStatus, pageable: Pageable): Page<InquiryJpaEntity>
}
