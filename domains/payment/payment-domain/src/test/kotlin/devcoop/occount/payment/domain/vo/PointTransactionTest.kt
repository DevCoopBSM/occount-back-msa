package devcoop.occount.payment.domain.vo

import kotlin.test.Test
import kotlin.test.assertEquals

class PointTransactionTest {
    @Test
    fun `calculate after point returns before plus transaction point`() {
        val transaction = PointTransaction(
            beforePoint = 1000,
            transactionPoint = -300,
            afterPoint = 700,
        )

        assertEquals(700, transaction.calculateAfterPoint())
        assertEquals(1000, transaction.beforePoint())
        assertEquals(-300, transaction.transactionPoint())
        assertEquals(700, transaction.afterPoint())
    }
}
