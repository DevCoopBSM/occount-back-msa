package devcoop.occount.inquiry.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InquiryJpaRepository : JpaRepository<InquiryJpaEntity, Long> {
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<InquiryJpaEntity>
}
