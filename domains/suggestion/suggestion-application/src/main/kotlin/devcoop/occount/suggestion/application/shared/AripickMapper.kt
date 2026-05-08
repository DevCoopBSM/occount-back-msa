package devcoop.occount.suggestion.application.shared

import devcoop.occount.suggestion.domain.aripick.AripickItem
import org.springframework.stereotype.Component

@Component
class AripickMapper {
    fun toResponse(aripickItem: AripickItem): AripickResponse {
        return AripickResponse(
            proposalId = aripickItem.getProposalId(),
            name = aripickItem.getName(),
            reason = aripickItem.getReason(),
            proposerId = aripickItem.getProposerId(),
            proposalDate = aripickItem.getProposalDate(),
            status = aripickItem.getStatus(),
            likeCount = aripickItem.getLike(),
        )
    }
}
