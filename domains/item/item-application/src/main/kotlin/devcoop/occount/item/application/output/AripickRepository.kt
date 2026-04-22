package devcoop.occount.item.application.output

import devcoop.occount.item.domain.aripick.AripickItem
import devcoop.occount.item.domain.aripick.AripickStatus

interface AripickRepository {
    fun findAll(): List<AripickItem>
    fun findById(proposalId: Long): AripickItem?
    fun save(aripickItem: AripickItem): AripickItem
    fun updateStatus(proposalId: Long, status: AripickStatus): Boolean
    fun deleteById(proposalId: Long)
    fun existsLike(proposalId: Long, userId: Long): Boolean
    fun saveLikeIfAbsent(proposalId: Long, userId: Long): Boolean
    fun deleteLike(proposalId: Long, userId: Long): Boolean
    fun increaseLikeCount(proposalId: Long): Boolean
    fun decreaseLikeCount(proposalId: Long): Boolean
    fun deleteLikesByProposalId(proposalId: Long)
    fun countAll(): Long
    fun countByStatus(status: AripickStatus): Long
}
