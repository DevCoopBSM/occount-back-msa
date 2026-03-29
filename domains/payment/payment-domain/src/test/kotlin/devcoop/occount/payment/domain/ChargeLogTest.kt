package devcoop.occount.payment.domain

import devcoop.occount.payment.domain.vo.PointTransaction
import kotlin.test.Test
import kotlin.test.assertEquals

class ChargeLogTest {
    @Test
    fun `charge log stores userId and chargeAmount`() {
        val chargeLog = ChargeLog(
            userId = 1L,
            chargeAmount = 5000,
            pointTransaction = PointTransaction(0, 5000),
        )

        assertEquals(1L, chargeLog.getUserId())
        assertEquals(5000, chargeLog.getChargeAmount())
    }
}
