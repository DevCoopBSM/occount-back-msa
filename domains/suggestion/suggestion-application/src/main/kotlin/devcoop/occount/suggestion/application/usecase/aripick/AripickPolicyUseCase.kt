package devcoop.occount.suggestion.application.usecase.aripick

import devcoop.occount.suggestion.application.output.AripickPolicyRepository
import devcoop.occount.suggestion.application.shared.AripickBlockedKeywordListResponse
import devcoop.occount.suggestion.application.shared.AripickBlockedKeywordResponse
import devcoop.occount.suggestion.domain.aripick.AripickBlockedKeywordAlreadyExistsException
import devcoop.occount.suggestion.domain.aripick.AripickInvalidBlockedKeywordException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class AripickPolicyUseCase(
    private val aripickPolicyRepository: AripickPolicyRepository,
) {
    fun getBlockedKeywords(): AripickBlockedKeywordListResponse {
        val keywords = aripickPolicyRepository.findBlockedKeywords()
            .map {
                AripickBlockedKeywordResponse(
                    keywordId = it.getKeywordId(),
                    keyword = it.getKeyword(),
                    registeredDate = it.getRegisteredDate(),
                )
            }
        return AripickBlockedKeywordListResponse(keywords = keywords)
    }

    fun blockKeyword(request: CreateAripickBlockedKeywordRequest): AripickBlockedKeywordResponse {
        val normalized = request.keyword.trim()
        if (normalized.isBlank()) {
            throw AripickInvalidBlockedKeywordException()
        }
        if (aripickPolicyRepository.existsBlockedKeyword(normalized)) {
            throw AripickBlockedKeywordAlreadyExistsException()
        }
        val saved = try {
            aripickPolicyRepository.saveBlockedKeyword(normalized)
        } catch (ex: DataIntegrityViolationException) {
            throw AripickBlockedKeywordAlreadyExistsException()
        }
        return AripickBlockedKeywordResponse(
            keywordId = saved.getKeywordId(),
            keyword = saved.getKeyword(),
            registeredDate = saved.getRegisteredDate(),
        )
    }

    fun unblockKeyword(keywordId: Long) {
        aripickPolicyRepository.deleteBlockedKeyword(keywordId)
    }
}
