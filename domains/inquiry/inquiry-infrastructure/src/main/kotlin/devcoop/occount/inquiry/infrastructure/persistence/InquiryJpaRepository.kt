package devcoop.occount.inquiry.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InquiryJpaRepository : JpaRepository<InquiryJpaEntity, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<InquiryJpaEntity>
}
