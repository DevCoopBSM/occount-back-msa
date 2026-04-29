package devcoop.occount.suggestion.application.output

import devcoop.occount.suggestion.domain.aripick.AripickBlockedKeyword

interface AripickPolicyRepository {
    fun hasBlockedKeyword(name: String): Boolean
    fun existsBlockedKeyword(keyword: String): Boolean
    fun findBlockedKeywords(): List<AripickBlockedKeyword>
    fun saveBlockedKeyword(keyword: String): AripickBlockedKeyword
    fun deleteBlockedKeyword(keywordId: Long)
}
