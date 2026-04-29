package devcoop.occount.suggestion.application.query

import devcoop.occount.suggestion.application.output.AripickRepository
import devcoop.occount.suggestion.application.shared.AripickMapper
import devcoop.occount.suggestion.application.shared.AripickResponse
import devcoop.occount.suggestion.domain.aripick.AripickNotFoundException
import devcoop.occount.suggestion.domain.aripick.AripickStatus
import org.springframework.stereotype.Service

@Service
class AripickQueryService(
    private val aripickRepository: AripickRepository,
    private val aripickMapper: AripickMapper,
) {
    fun getAllItems(): AripickListResponse {
        return AripickListResponse(
            aripickItems = aripickRepository.findAll().map(aripickMapper::toResponse),
        )
    }

    fun getItem(proposalId: Long): AripickResponse {
        val proposal = aripickRepository.findById(proposalId)
            ?: throw AripickNotFoundException()
        return aripickMapper.toResponse(proposal)
    }

    fun getStats(): AripickStatsResponse {
        return AripickStatsResponse(
            totalProposals = aripickRepository.countAll(),
            approved = aripickRepository.countByStatus(AripickStatus.APPROVED),
            pending = aripickRepository.countByStatus(AripickStatus.PENDING),
            rejected = aripickRepository.countByStatus(AripickStatus.REJECTED),
        )
    }
}
