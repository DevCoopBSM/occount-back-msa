package devcoop.occount.item.infrastructure.persistence.aripick

import devcoop.occount.item.domain.aripick.AripickItem

object AripickPersistenceMapper {
    fun toDomain(entity: AripickJpaEntity): AripickItem {
        return AripickItem(
            proposalId = entity.getProposalId(),
            name = entity.getName(),
            reason = entity.getReason(),
            proposalDate = entity.getProposalDate(),
            proposerId = entity.getProposerId(),
            status = entity.getStatus(),
            like = entity.getLikeCount(),
        )
    }

    fun toEntity(domain: AripickItem): AripickJpaEntity {
        return AripickJpaEntity(
            proposalId = domain.getProposalId(),
            name = domain.getName(),
            reason = domain.getReason(),
            proposalDate = domain.getProposalDate(),
            proposerId = domain.getProposerId(),
            status = domain.getStatus(),
            likeCount = domain.getLike(),
        )
    }
}
