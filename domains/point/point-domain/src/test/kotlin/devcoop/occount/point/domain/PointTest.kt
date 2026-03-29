package devcoop.occount.point.domain

import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PointTest {
    @Test
    @DisplayName("충전 시 잔액에 금액이 더해진다")
    fun `charge adds amount to balance`() {
        val point = Point(userId = 1L, balance = 100)

        val chargedPoint = point.charge(30)

        assertEquals(130, chargedPoint.balance)
    }

    @Test
    @DisplayName("충전 시 0 이하 금액은 허용하지 않는다")
    fun `charge rejects non positive amount`() {
        val point = Point(userId = 1L, balance = 100)

        assertFailsWith<InvalidPointAmountException> {
            point.charge(0)
        }
    }

    @Test
    @DisplayName("차감 시 잔액이 부족하면 예외가 발생한다")
    fun `deduct rejects insufficient balance`() {
        val point = Point(userId = 1L, balance = 100)

        assertFailsWith<InsufficientPointBalanceException> {
            point.deduct(101)
        }
    }

    @Test
    @DisplayName("차감 시 잔액에서 금액이 빠진다")
    fun `deduct subtracts amount from balance`() {
        val point = Point(userId = 1L, balance = 100)

        val deductedPoint = point.deduct(40)

        assertEquals(60, deductedPoint.balance)
    }
}
