package devcoop.occount.payment.domain.vo

import kotlin.test.Test
import kotlin.test.assertEquals

class PointTransactionTest {
    @Test
    fun `point transaction stores before and after point`() {
        val transaction = PointTransaction(
            beforePoint = 1000,
            afterPoint = 700,
        )

        assertEquals(1000, transaction.beforePoint())
        assertEquals(700, transaction.afterPoint())
    }
}
