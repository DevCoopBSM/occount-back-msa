package devcoop.occount.payment.application.query.chargelog

import devcoop.occount.payment.application.shared.PointTransactionResult
import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.ChargeLogRepository
import devcoop.occount.payment.domain.type.RefundState
import devcoop.occount.payment.domain.vo.PointTransaction
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class GetChargeHistoryQueryServiceTest {
    @Test
    fun `getChargeHistory delegates to repository`() {
        val chargeLogs = listOf(
            ChargeLog(
                userId = 1L,
                chargeAmount = 1000,
                pointTransaction = PointTransaction(0, 1000, 1000),
            ),
        )
        val service = GetChargeHistoryQueryService(StubChargeLogRepository(chargeLogs))

        val result = service.getChargeHistory(1L)

        assertEquals(
            listOf(
                ChargeLogResult(
                    chargeId = chargeLogs.single().getChargeId(),
                    userId = chargeLogs.single().getUserId(),
                    chargeDate = chargeLogs.single().getChargeDate(),
                    chargeAmount = chargeLogs.single().getChargeAmount(),
                    pointTransaction = PointTransactionResult(
                        beforePoint = 0,
                        transactionPoint = 1000,
                        afterPoint = 1000,
                    ),
                    cardInfo = null,
                    transactionInfo = null,
                    managedEmail = null,
                    reason = null,
                    refundState = RefundState.NONE,
                    refundDate = null,
                    refundRequesterId = null,
                ),
            ),
            result,
        )
    }

    private class StubChargeLogRepository(
        private val logs: List<ChargeLog>,
    ) : ChargeLogRepository {
        override fun findByUserId(userId: Long): List<ChargeLog> = logs
        override fun findByPaymentId(paymentId: String): ChargeLog? = logs.firstOrNull()
        override fun findByRefundState(refundState: RefundState): List<ChargeLog> = logs
        override fun findByUserIdAndChargeDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<ChargeLog> = logs
        override fun save(chargeLog: ChargeLog): ChargeLog = chargeLog
        override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> = chargeLogs
    }
}
