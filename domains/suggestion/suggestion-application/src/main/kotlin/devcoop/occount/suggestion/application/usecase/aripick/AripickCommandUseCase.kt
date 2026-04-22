package devcoop.occount.suggestion.application.usecase.aripick

import devcoop.occount.suggestion.application.output.AripickRepository
import devcoop.occount.suggestion.application.output.AripickPolicyRepository
import devcoop.occount.suggestion.application.output.FoodSafetyRepository
import devcoop.occount.suggestion.application.shared.AripickMapper
import devcoop.occount.suggestion.application.shared.AripickResponse
import devcoop.occount.suggestion.domain.aripick.AripickAccessDeniedException
import devcoop.occount.suggestion.domain.aripick.AripickItem
import devcoop.occount.suggestion.domain.aripick.AripickNotFoundException
import devcoop.occount.suggestion.domain.aripick.AripickPolicyViolationException
import devcoop.occount.suggestion.domain.aripick.AripickStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AripickCommandUseCase(
    private val aripickRepository: AripickRepository,
    private val aripickPolicyRepository: AripickPolicyRepository,
    private val foodSafetyRepository: FoodSafetyRepository,
    private val aripickMapper: AripickMapper,
) {
    fun create(
        request: CreateAripickRequest,
        proposerId: Long,
    ): AripickResponse {
        val detail = foodSafetyRepository.getDetail(request.typeNSeq)
            ?: throw AripickPolicyViolationException()

        if (!detail.isAllowed) {
            throw AripickPolicyViolationException()
        }

        if (aripickPolicyRepository.hasBlockedKeyword(detail.name)) {
            throw AripickPolicyViolationException()
        }

        val created = aripickRepository.save(
            AripickItem(
                name = detail.name,
                reason = request.reason,
                proposerId = proposerId,
            ),
        )
        return aripickMapper.toResponse(created)
    }

    @Transactional
    fun approve(proposalId: Long): AripickResponse {
        val proposal = getProposal(proposalId)
        changeStatus(proposalId, proposal.approve().getStatus())
        val approved = getProposal(proposalId)
        return aripickMapper.toResponse(approved)
    }

    @Transactional
    fun reject(proposalId: Long): AripickResponse {
        val proposal = getProposal(proposalId)
        changeStatus(proposalId, proposal.reject().getStatus())
        val rejected = getProposal(proposalId)
        return aripickMapper.toResponse(rejected)
    }

    @Transactional
    fun pending(proposalId: Long): AripickResponse {
        val proposal = getProposal(proposalId)
        changeStatus(proposalId, proposal.pending().getStatus())
        val pending = getProposal(proposalId)
        return aripickMapper.toResponse(pending)
    }

    @Transactional
    fun delete(
        proposalId: Long,
        requesterId: Long,
    ) {
        val proposal = getProposal(proposalId)
        if (!proposal.canDeleteBy(requesterId)) {
            throw AripickAccessDeniedException()
        }

        aripickRepository.deleteLikesByProposalId(proposalId)
        aripickRepository.deleteById(proposalId)
    }

    @Transactional
    fun deleteAsAdmin(proposalId: Long) {
        getProposal(proposalId)
        aripickRepository.deleteLikesByProposalId(proposalId)
        aripickRepository.deleteById(proposalId)
    }

    @Transactional
    fun toggleLike(
        proposalId: Long,
        userId: Long,
    ): AripickLikeToggleResponse {
        getProposal(proposalId)
        val alreadyLiked = aripickRepository.existsLike(proposalId, userId)

        if (alreadyLiked) {
            if (aripickRepository.deleteLike(proposalId, userId)) {
                aripickRepository.decreaseLikeCount(proposalId)
            }
        } else {
            if (aripickRepository.saveLikeIfAbsent(proposalId, userId)) {
                aripickRepository.increaseLikeCount(proposalId)
            }
        }

        val updated = getProposal(proposalId)
        val liked = aripickRepository.existsLike(proposalId, userId)

        return AripickLikeToggleResponse(
            proposalId = proposalId,
            liked = liked,
            likeCount = updated.getLike(),
        )
    }

    private fun getProposal(proposalId: Long): AripickItem {
        return aripickRepository.findById(proposalId)
            ?: throw AripickNotFoundException()
    }

    private fun changeStatus(
        proposalId: Long,
        status: AripickStatus,
    ) {
        if (!aripickRepository.updateStatus(proposalId, status)) {
            throw AripickNotFoundException()
        }
    }
}
