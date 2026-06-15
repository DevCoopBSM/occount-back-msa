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
import kotlin.test.assertTrue

class GetChargeHistoryQueryServiceTest {
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
    @DisplayName("본인(userId)의 충전 내역만 반환하고 타인 것은 제외한다")
    fun `returns only own charge logs`() {
        val logs = listOf(
            chargeLog(chargeId = 1L, userId = 1L),
            chargeLog(chargeId = 2L, userId = 2L),
            chargeLog(chargeId = 3L, userId = 1L),
            chargeLog(chargeId = 4L, userId = 3L),
        )
        val service = GetChargeHistoryQueryService(FakeChargeLogRepository(initialLogs = logs))

        val response = service.getChargeHistory(userId = 1L, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "chargeId")))

        assertEquals(listOf(3L, 1L), response.charges.map { it.chargeId })
        assertEquals(2L, response.totalCount)
    }

    @Test
    @DisplayName("페이징 메타데이터와 함께 본인 충전 내역을 반환한다")
    fun `returns own charge logs with pagination metadata`() {
        val logs = (1L..25L).map { chargeLog(chargeId = it, userId = 1L) } +
            (26L..30L).map { chargeLog(chargeId = it, userId = 2L) }
        val service = GetChargeHistoryQueryService(FakeChargeLogRepository(initialLogs = logs))

        val response = service.getChargeHistory(userId = 1L, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "chargeId")))

        assertEquals(10, response.charges.size)
        assertEquals(25L, response.totalCount)
        assertEquals(3, response.totalPages)
        assertEquals(0, response.currentPage)
        assertEquals(10, response.pageSize)
    }

    @Test
    @DisplayName("충전 정보 필드(사유, 포인트 변화)를 매핑한다")
    fun `maps charge fields including point transaction`() {
        val log = chargeLog(
            chargeId = 7L,
            userId = 42L,
            reason = ChargeReason.PURCHASE,
            detailReason = "충전",
            paymentId = 777L,
            before = 500,
            change = 1500,
        )
        val service = GetChargeHistoryQueryService(FakeChargeLogRepository(initialLogs = listOf(log)))

        val result = service.getChargeHistory(userId = 42L, PageRequest.of(0, 10)).charges.single()

        assertEquals(7L, result.chargeId)
        assertEquals(ChargeReason.PURCHASE, result.chargeReason)
        assertEquals("충전", result.detailReason)
        assertEquals(777L, result.paymentId)
        assertEquals(500, result.beforePoint)
        assertEquals(1500, result.changeAmount)
        assertEquals(2000, result.afterPoint)
    }

    @Test
    @DisplayName("본인 내역이 없으면 빈 목록과 0 메타데이터를 반환한다")
    fun `returns empty list when user has no charge logs`() {
        val logs = listOf(chargeLog(chargeId = 1L, userId = 2L))
        val service = GetChargeHistoryQueryService(FakeChargeLogRepository(initialLogs = logs))

        val response = service.getChargeHistory(userId = 1L, PageRequest.of(0, 10))

        assertTrue(response.charges.isEmpty())
        assertEquals(0L, response.totalCount)
        assertEquals(0, response.totalPages)
    }
}
