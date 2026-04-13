package devcoop.occount.payment.domain

import devcoop.occount.payment.domain.wallet.InsufficientPointsException
import devcoop.occount.payment.domain.wallet.InvalidChargeAmountException
import devcoop.occount.payment.domain.wallet.Wallet
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WalletTest {
    @Test
    @DisplayName("충전 시 포인트에 금액이 더해진다")
    fun `charge adds amount to point`() {
        val wallet = Wallet(userId = 1L, point = 100)

        val chargedWallet = wallet.charge(30)

        assertEquals(130, chargedWallet.point)
    }

    @Test
    @DisplayName("충전 시 0 이하 금액은 허용하지 않는다")
    fun `charge rejects non positive amount`() {
        val wallet = Wallet(userId = 1L, point = 100)

        assertFailsWith<InvalidChargeAmountException> {
            wallet.charge(0)
        }
    }

    @Test
    @DisplayName("차감 시 포인트가 부족하면 예외가 발생한다")
    fun `deduct rejects insufficient point`() {
        val wallet = Wallet(userId = 1L, point = 100)

        assertFailsWith<InsufficientPointsException> {
            wallet.deduct(101)
        }
    }

    @Test
    @DisplayName("차감 시 포인트에서 금액이 빠진다")
    fun `deduct subtracts amount from point`() {
        val wallet = Wallet(userId = 1L, point = 100)

        val deductedWallet = wallet.deduct(40)

        assertEquals(60, deductedWallet.point)
    }
}
