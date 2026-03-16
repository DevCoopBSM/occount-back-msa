package devcoop.occount.point.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PointTest {
    @Test
    fun `charge adds amount to balance`() {
        val point = Point(userId = 1L, balance = 100)

        val chargedPoint = point.charge(30)

        assertEquals(130, chargedPoint.balance)
    }

    @Test
    fun `charge rejects non positive amount`() {
        val point = Point(userId = 1L, balance = 100)

        assertFailsWith<InvalidPointAmountException> {
            point.charge(0)
        }
    }

    @Test
    fun `deduct rejects insufficient balance`() {
        val point = Point(userId = 1L, balance = 100)

        assertFailsWith<InsufficientPointBalanceException> {
            point.deduct(101)
        }
    }

    @Test
    fun `deduct subtracts amount from balance`() {
        val point = Point(userId = 1L, balance = 100)

        val deductedPoint = point.deduct(40)

        assertEquals(60, deductedPoint.balance)
    }
}
