package devcoop.occount.payment.domain

import devcoop.occount.payment.domain.type.RefundState
import devcoop.occount.payment.domain.vo.PointTransaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChargeLogTest {
    @Test
    fun `request refund updates refund state and requester`() {
        val chargeLog = ChargeLog(
            userId = 1L,
            chargeAmount = 5000,
            pointTransaction = PointTransaction(0, 5000, 5000),
        )

        chargeLog.requestRefund("admin-1")

        assertEquals(RefundState.REQUESTED, chargeLog.getRefundState())
        assertEquals("admin-1", chargeLog.getRefundRequesterId())
    }

    @Test
    fun `complete refund marks refund completed and sets refund date`() {
        val chargeLog = ChargeLog(
            userId = 1L,
            chargeAmount = 5000,
            pointTransaction = PointTransaction(0, 5000, 5000),
        )

        chargeLog.completeRefund()

        assertEquals(RefundState.COMPLETED, chargeLog.getRefundState())
        assertNotNull(chargeLog.getRefundDate())
    }
}
