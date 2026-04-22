package devcoop.occount.suggestion.application.usecase.aripick

import devcoop.occount.suggestion.application.output.AripickPolicyRepository
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
        val saved = aripickPolicyRepository.saveBlockedKeyword(request.keyword.trim())
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
