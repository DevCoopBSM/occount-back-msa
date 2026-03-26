package devcoop.occount.payment.application.payment

import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.type.PaymentType
import devcoop.occount.payment.domain.type.RefundState
import devcoop.occount.payment.domain.vo.PointTransaction
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentHistoryTest {
    @Test
    fun `history service delegates to repositories`() {
        val paymentLogs = listOf(
            PaymentLog(
                userId = 1L,
                paymentType = PaymentType.POINT,
                totalAmount = 1000,
            ),
        )
        val chargeLogs = listOf(
            ChargeLog(
                userId = 1L,
                chargeAmount = 1000,
                pointTransaction = PointTransaction(0, 1000, 1000),
            ),
        )
        val service = PaymentService(
            paymentLogRepository = StubPaymentLogRepository(paymentLogs),
            chargeLogRepository = StubChargeLogRepository(chargeLogs),
            memberPaymentReadPort = object : MemberPaymentReadPort {
                override fun getUser(userId: Long): PaymentUserInfo = error("unused")
            },
            pointWalletPort = object : PointWalletPort {
                override fun getBalance(userId: Long): Int = error("unused")
                override fun charge(userId: Long, amount: Int): Int = error("unused")
                override fun deduct(userId: Long, amount: Int): Int = error("unused")
            },
            cardPaymentPort = object : CardPaymentPort {
                override fun approve(
                    amount: Int,
                    items: List<devcoop.occount.payment.application.dto.request.ItemInfo>,
                ): devcoop.occount.payment.application.dto.response.PgResponse = error("unused")
            },
        )

        assertEquals(paymentLogs, service.getPaymentHistory(1L))
        assertEquals(chargeLogs, service.getChargeHistory(1L))
    }

    private class StubPaymentLogRepository(
        private val logs: List<PaymentLog>,
    ) : PaymentLogRepository {
        override fun findByUserId(userId: Long): List<PaymentLog> = logs

        override fun findByUserIdAndPaymentDateBetween(
            userId: Long,
            startDate: LocalDateTime,
            endDate: LocalDateTime,
        ): List<PaymentLog> = logs

        override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> = logs

        override fun save(paymentLog: PaymentLog): PaymentLog = paymentLog

        override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> = paymentLogs
    }

    private class StubChargeLogRepository(
        private val logs: List<ChargeLog>,
    ) : ChargeLogRepository {
        override fun findByUserId(userId: Long): List<ChargeLog> = logs

        override fun findByPaymentId(paymentId: String): ChargeLog? = logs.firstOrNull()

        override fun findByRefundState(refundState: RefundState): List<ChargeLog> = logs

        override fun findByUserIdAndChargeDateBetween(
            userId: Long,
            startDate: LocalDateTime,
            endDate: LocalDateTime,
        ): List<ChargeLog> = logs

        override fun save(chargeLog: ChargeLog): ChargeLog = chargeLog

        override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> = chargeLogs
    }
}
