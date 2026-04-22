package devcoop.occount.suggestion.application.support

import devcoop.occount.suggestion.application.output.AripickRepository
import devcoop.occount.suggestion.domain.aripick.AripickItem
import devcoop.occount.suggestion.domain.aripick.AripickStatus
import java.time.LocalDate

fun aripickFixture(
    proposalId: Long,
    name: String = "제로콜라 355ml",
    reason: String = "학생 수요가 많음",
    proposalDate: LocalDate = LocalDate.of(2026, 4, 22),
    proposerId: Long = 1L,
    status: AripickStatus = AripickStatus.검토중,
    like: Int = 0,
): AripickItem {
    return AripickItem(
        proposalId = proposalId,
        name = name,
        reason = reason,
        proposalDate = proposalDate,
        proposerId = proposerId,
        status = status,
        like = like,
    )
}

class FakeAripickRepository(
    initialItems: List<AripickItem> = emptyList(),
) : AripickRepository {
    private val itemsById = linkedMapOf<Long, AripickItem>().apply {
        initialItems.forEach { put(it.getProposalId(), it) }
    }

    val likes = mutableSetOf<Pair<Long, Long>>()
    var deletedProposalIds: MutableList<Long> = mutableListOf()
    var deletedLikeProposalIds: MutableList<Long> = mutableListOf()

    override fun findAll(): List<AripickItem> = itemsById.values.toList()

    override fun findById(proposalId: Long): AripickItem? = itemsById[proposalId]

    override fun save(aripickItem: AripickItem): AripickItem {
        val persisted = if (aripickItem.getProposalId() == 0L) {
            val nextId = (itemsById.keys.maxOrNull() ?: 0L) + 1L
            AripickItem(
                proposalId = nextId,
                name = aripickItem.getName(),
                reason = aripickItem.getReason(),
                proposalDate = aripickItem.getProposalDate(),
                proposerId = aripickItem.getProposerId(),
                status = aripickItem.getStatus(),
                like = aripickItem.getLike(),
            )
        } else {
            aripickItem
        }

        itemsById[persisted.getProposalId()] = persisted
        return persisted
    }

    override fun updateStatus(proposalId: Long, status: AripickStatus): Boolean {
        val current = itemsById[proposalId] ?: return false
        itemsById[proposalId] = when (status) {
            AripickStatus.검토중 -> current.pending()
            AripickStatus.승인됨 -> current.approve()
            AripickStatus.거절됨 -> current.reject()
        }
        return true
    }

    override fun deleteById(proposalId: Long) {
        deletedProposalIds.add(proposalId)
        itemsById.remove(proposalId)
    }

    override fun existsLike(proposalId: Long, userId: Long): Boolean {
        return likes.contains(proposalId to userId)
    }

    override fun saveLikeIfAbsent(proposalId: Long, userId: Long): Boolean {
        return likes.add(proposalId to userId)
    }

    override fun deleteLike(proposalId: Long, userId: Long): Boolean {
        return likes.remove(proposalId to userId)
    }

    override fun increaseLikeCount(proposalId: Long): Boolean {
        val current = itemsById[proposalId] ?: return false
        itemsById[proposalId] = current.toggleLike(alreadyLiked = false)
        return true
    }

    override fun decreaseLikeCount(proposalId: Long): Boolean {
        val current = itemsById[proposalId] ?: return false
        itemsById[proposalId] = current.toggleLike(alreadyLiked = true)
        return true
    }

    override fun deleteLikesByProposalId(proposalId: Long) {
        deletedLikeProposalIds.add(proposalId)
        likes.removeIf { it.first == proposalId }
    }

    override fun countAll(): Long = itemsById.size.toLong()

    override fun countByStatus(status: AripickStatus): Long {
        return itemsById.values.count { it.getStatus() == status }.toLong()
    }
}
