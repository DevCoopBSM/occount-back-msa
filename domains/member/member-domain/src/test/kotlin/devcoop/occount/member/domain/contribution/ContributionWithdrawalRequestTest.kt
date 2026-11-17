package devcoop.occount.member.domain.contribution

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class ContributionWithdrawalRequestTest {
    @Test
    fun `processing records actor and time while preserving version and Long amount`() {
        val pending = ContributionWithdrawalRequest.request(1L, Long.MAX_VALUE).copy(version = 4L)
        assertEquals(null, pending.processedBy)
        assertEquals(null, pending.processedAt)
        val approved = pending.approve(99L)
        val rejected = pending.reject(88L, "reason")
        assertEquals(Long.MAX_VALUE, approved.amount)
        assertEquals(4L, approved.version)
        assertEquals(4L, rejected.version)
        assertEquals(99L, approved.processedBy)
        assertEquals(88L, rejected.processedBy)
        assertNotNull(approved.processedAt)
        assertNotNull(rejected.processedAt)
        assertEquals("reason", rejected.rejectionReason)
        assertFailsWith<ContributionWithdrawalRequestAlreadyProcessedException> { approved.approve(77L) }
        assertFailsWith<ContributionWithdrawalRequestAlreadyProcessedException> { approved.reject(77L) }
        assertFailsWith<ContributionWithdrawalRequestAlreadyProcessedException> { rejected.approve(77L) }
        assertFailsWith<ContributionWithdrawalRequestAlreadyProcessedException> { rejected.reject(77L) }
    }

    @Test
    fun `request must have positive amount`() {
        assertFailsWith<InvalidContributionWithdrawalAmountException> { ContributionWithdrawalRequest.request(1L, 0L) }
        assertFailsWith<InvalidContributionWithdrawalAmountException> { ContributionWithdrawalRequest.request(1L, -1L) }
    }
}
