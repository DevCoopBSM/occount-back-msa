package devcoop.occount.inquiry.infrastructure.persistence

import devcoop.occount.inquiry.domain.inquiry.Inquiry

object InquiryPersistenceMapper {
    fun toDomain(entity: InquiryJpaEntity): Inquiry = Inquiry(
        id = entity.id,
        userId = entity.userId,
        title = entity.title,
        content = entity.content,
        category = entity.category,
        status = entity.status,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    fun toEntity(inquiry: Inquiry): InquiryJpaEntity = InquiryJpaEntity(
        id = inquiry.id,
        userId = inquiry.userId,
        title = inquiry.title,
        content = inquiry.content,
        category = inquiry.category,
        status = inquiry.status,
        createdAt = inquiry.createdAt,
        updatedAt = inquiry.updatedAt,
    )
}
