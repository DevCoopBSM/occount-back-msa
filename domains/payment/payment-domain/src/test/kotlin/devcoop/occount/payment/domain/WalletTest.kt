package devcoop.occount.payment.domain

import devcoop.occount.payment.domain.exception.InsufficientPointBalanceException
import devcoop.occount.payment.domain.exception.InvalidPointAmountException
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WalletTest {
    @Test
    @DisplayName("충전 시 잔액에 금액이 더해진다")
    fun `charge adds amount to balance`() {
        val pointWallet = Wallet(userId = 1L, balance = 100)

        val chargedWallet = pointWallet.charge(30)

        assertEquals(130, chargedWallet.balance)
    }

    @Test
    @DisplayName("충전 시 0 이하 금액은 허용하지 않는다")
    fun `charge rejects non positive amount`() {
        val pointWallet = Wallet(userId = 1L, balance = 100)

        assertFailsWith<InvalidPointAmountException> {
            pointWallet.charge(0)
        }
    }

    @Test
    @DisplayName("차감 시 잔액이 부족하면 예외가 발생한다")
    fun `deduct rejects insufficient balance`() {
        val pointWallet = Wallet(userId = 1L, balance = 100)

        assertFailsWith<InsufficientPointBalanceException> {
            pointWallet.deduct(101)
        }
    }

    @Test
    @DisplayName("차감 시 잔액에서 금액이 빠진다")
    fun `deduct subtracts amount from balance`() {
        val pointWallet = Wallet(userId = 1L, balance = 100)

        val deductedWallet = pointWallet.deduct(40)

        assertEquals(60, deductedWallet.balance)
    }
}
