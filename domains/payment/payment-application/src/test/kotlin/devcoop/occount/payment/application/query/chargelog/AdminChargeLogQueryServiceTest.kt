package devcoop.occount.payment.application.query.chargelog

import devcoop.occount.payment.application.support.FakeChargeLogRepository
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.ChargeReason
import devcoop.occount.payment.domain.wallet.PointTransaction
import org.junit.jupiter.api.DisplayName
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminChargeLogQueryServiceTest {
    private fun chargeLog(
        chargeId: Long,
        userId: Long = 1L,
        reason: ChargeReason = ChargeReason.ADMIN,
        detailReason: String? = "보상",
        chargedBy: Long? = 99L,
        paymentId: Long? = null,
        before: Int = 0,
        change: Int = 1000,
    ): ChargeLog = ChargeLog(
        chargeId = chargeId,
        userId = userId,
        chargeDate = LocalDateTime.of(2026, 6, 11, 9, 0),
        paymentId = paymentId,
        pointTransaction = PointTransaction(beforePoint = before, changeAmount = change, afterPoint = before + change),
        chargeReason = reason,
        detailReason = detailReason,
        chargedBy = chargedBy,
    )

    @Test
    @DisplayName("페이징 메타데이터와 함께 충전 내역을 반환한다")
    fun `returns charge logs with pagination metadata`() {
        val logs = (1L..25L).map { chargeLog(chargeId = it) }
        val service = AdminChargeLogQueryService(FakeChargeLogRepository(initialLogs = logs))

        val response = service.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "chargeId")))

        assertEquals(10, response.charges.size)
        assertEquals(25L, response.totalCount)
        assertEquals(3, response.totalPages)
        assertEquals(0, response.currentPage)
        assertEquals(10, response.pageSize)
    }

    @Test
    @DisplayName("chargeId 내림차순(최신순)으로 정렬되어 반환된다")
    fun `returns charges sorted by chargeId descending`() {
        val logs = (1L..3L).map { chargeLog(chargeId = it) }
        val service = AdminChargeLogQueryService(FakeChargeLogRepository(initialLogs = logs))

        val response = service.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "chargeId")))

        assertEquals(listOf(3L, 2L, 1L), response.charges.map { it.chargeId })
    }

    @Test
    @DisplayName("감사에 필요한 충전 필드(충전자 id, 사유, 포인트 변화)를 모두 매핑한다")
    fun `maps audit fields including charger id and point transaction`() {
        val log = chargeLog(
            chargeId = 7L,
            userId = 42L,
            reason = ChargeReason.ADMIN,
            detailReason = "5월 우수회원 보상",
            chargedBy = 100L,
            before = 500,
            change = 1500,
        )
        val service = AdminChargeLogQueryService(FakeChargeLogRepository(initialLogs = listOf(log)))

        val result = service.findAll(PageRequest.of(0, 10)).charges.single()

        assertEquals(7L, result.chargeId)
        assertEquals(42L, result.userId)
        assertEquals(ChargeReason.ADMIN, result.chargeReason)
        assertEquals("5월 우수회원 보상", result.detailReason)
        assertEquals(100L, result.chargedBy)
        assertEquals(500, result.beforePoint)
        assertEquals(1500, result.changeAmount)
        assertEquals(2000, result.afterPoint)
    }

    @Test
    @DisplayName("내역이 없으면 빈 목록과 0 메타데이터를 반환한다")
    fun `returns empty list when no charge logs exist`() {
        val service = AdminChargeLogQueryService(FakeChargeLogRepository())

        val response = service.findAll(PageRequest.of(0, 10))

        assertEquals(emptyList(), response.charges)
        assertEquals(0L, response.totalCount)
        assertEquals(0, response.totalPages)
    }

    @Test
    @DisplayName("어드민이 아닌 충전(chargedBy=null, paymentId 존재)도 그대로 매핑한다")
    fun `maps system charge with null chargedBy and payment id`() {
        val log = chargeLog(chargeId = 5L, reason = ChargeReason.PURCHASE, chargedBy = null, paymentId = 777L)
        val service = AdminChargeLogQueryService(FakeChargeLogRepository(initialLogs = listOf(log)))

        val result = service.findAll(PageRequest.of(0, 10)).charges.single()

        assertEquals(ChargeReason.PURCHASE, result.chargeReason)
        assertNull(result.chargedBy)
        assertEquals(777L, result.paymentId)
    }
}
