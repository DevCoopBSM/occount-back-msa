package devcoop.occount.inquiry.infrastructure.persistence

import devcoop.occount.inquiry.application.output.InquiryRepository
import devcoop.occount.inquiry.domain.inquiry.Inquiry
import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class InquiryRepositoryAdapter(
    private val inquiryJpaRepository: InquiryJpaRepository,
) : InquiryRepository {
    override fun save(inquiry: Inquiry): Inquiry {
        val entity = InquiryPersistenceMapper.toEntity(inquiry)
        return InquiryPersistenceMapper.toDomain(inquiryJpaRepository.save(entity))
    }

    override fun findPageByUserId(userId: Long, pageable: Pageable): Page<Inquiry> =
        inquiryJpaRepository.findAllByUserId(userId, pageable)
            .map(InquiryPersistenceMapper::toDomain)

    override fun findById(id: Long): Inquiry? =
        inquiryJpaRepository.findById(id).orElse(null)
            ?.let(InquiryPersistenceMapper::toDomain)

    override fun findPage(status: InquiryStatus?, pageable: Pageable): Page<Inquiry> =
        (if (status == null) {
            inquiryJpaRepository.findAll(pageable)
        } else {
            inquiryJpaRepository.findAllByStatus(status, pageable)
        }).map(InquiryPersistenceMapper::toDomain)
}
