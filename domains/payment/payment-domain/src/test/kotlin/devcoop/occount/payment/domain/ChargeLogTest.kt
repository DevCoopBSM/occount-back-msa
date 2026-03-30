package devcoop.occount.payment.domain

import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.ChargeReason
import devcoop.occount.payment.domain.wallet.PointTransaction
import kotlin.test.Test
import kotlin.test.assertEquals

class ChargeLogTest {
    @Test
    fun `charge log stores userId and pointTransaction`() {
        val chargeLog = ChargeLog(
            userId = 1L,
            pointTransaction = PointTransaction(
                beforePoint = 0,
                changeAmount = 5000,
                afterPoint = 5000,
            ),
            chargeReason = ChargeReason.PURCHASE,
        )

        assertEquals(1L, chargeLog.userId)
        assertEquals(5000, chargeLog.pointTransaction.changeAmount)
    }
}
