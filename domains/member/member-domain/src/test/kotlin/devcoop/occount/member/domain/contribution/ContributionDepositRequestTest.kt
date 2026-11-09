package devcoop.occount.member.domain.contribution

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ContributionDepositRequest 도메인 테스트")
class ContributionDepositRequestTest {
    @Test
    fun `approval and rejection preserve versions and record actors only when processed`() {
        val pending = ContributionDepositRequest.request(1L, 3_000_000_000L).copy(version = 7L)
        assertEquals(null, pending.processedBy)
        assertEquals(null, pending.processedAt)
        val approved = pending.approve(99L)
        val rejected = pending.reject(88L, "reason")
        assertEquals(7L, approved.version)
        assertEquals(7L, rejected.version)
        assertEquals(99L, approved.processedBy)
        assertEquals(88L, rejected.processedBy)
        assertEquals("reason", rejected.rejectionReason)
        kotlin.test.assertNotNull(approved.processedAt)
        kotlin.test.assertNotNull(rejected.processedAt)
        assertFailsWith<ContributionDepositRequestAlreadyProcessedException> { rejected.approve(99L) }
        assertFailsWith<ContributionDepositRequestAlreadyProcessedException> { approved.reject(99L) }
        assertFailsWith<ContributionDepositRequestAlreadyProcessedException> { rejected.reject(99L) }
    }

    @Test
    @DisplayName("출자금 납입 신청은 대기 상태로 생성된다")
    fun `request creates pending contribution deposit request`() {
        val request = ContributionDepositRequest.request(
            userId = 1L,
            amount = 10000,
        )

        assertEquals(1L, request.userId)
        assertEquals(10000L, request.amount)
        assertEquals(ContributionDepositRequestStatus.PENDING, request.status)
    }

    @Test
    @DisplayName("출자금 납입 신청 금액은 0보다 커야 한다")
    fun `request amount must be positive`() {
        assertFailsWith<InvalidContributionDepositAmountException> {
            ContributionDepositRequest.request(
                userId = 1L,
                amount = 0,
            )
        }
    }

    @Test
    @DisplayName("대기 상태 출자금 납입 신청을 승인 상태로 변경한다")
    fun `approve changes pending request to approved`() {
        val request = ContributionDepositRequest.request(userId = 1L, amount = 10000)

        val approved = request.approve(processedBy = 99L)

        assertEquals(ContributionDepositRequestStatus.APPROVED, approved.status)
    }

    @Test
    @DisplayName("이미 처리된 출자금 납입 신청은 다시 승인할 수 없다")
    fun `approve fails when request already processed`() {
        val approved = ContributionDepositRequest.request(userId = 1L, amount = 10000).approve(processedBy = 99L)

        assertFailsWith<ContributionDepositRequestAlreadyProcessedException> {
            approved.approve(processedBy = 99L)
        }
    }
}
