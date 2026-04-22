package devcoop.occount.suggestion.domain.aripick

import java.time.LocalDate

data class AripickItem(
    private val proposalId: Long = 0L,
    private val name: String = "",
    private val reason: String = "",
    private val proposalDate: LocalDate = LocalDate.now(),
    private val proposerId: Long = 0L,
    private val status: AripickStatus = AripickStatus.검토중,
    private val like: Int = 0
) {
    fun getProposalId() = proposalId
    fun getName() = name
    fun getReason() = reason
    fun getProposalDate() = proposalDate
    fun getProposerId() = proposerId
    fun getStatus() = status
    fun getLike() = like

    fun toggleLike(alreadyLiked: Boolean): AripickItem {
        return if (alreadyLiked) {
            copy(like = (like - 1).coerceAtLeast(0))
        } else {
            copy(like = like + 1)
        }
    }

    fun approve(): AripickItem {
        return copy(status = AripickStatus.승인됨)
    }

    fun reject(): AripickItem {
        return copy(status = AripickStatus.거절됨)
    }

    fun pending(): AripickItem {
        return copy(status = AripickStatus.검토중)
    }

    fun canDeleteBy(
        requesterId: Long,
    ): Boolean {
        return proposerId == requesterId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AripickItem) return false

        if (proposalId != other.proposalId) return false

        return true
    }

    override fun hashCode(): Int {
        return proposalId.hashCode()
    }
}
