package devcoop.occount.payment.application.payment

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.PgResult
import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.ChargeLogRepository
import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.PaymentLogRepository
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
                    items: List<ItemCommand>,
                ): PgResult = error("unused")
            },
        )

        assertEquals(
            listOf(
                PaymentLogResult(
                    paymentId = paymentLogs.single().getPaymentId(),
                    userId = paymentLogs.single().getUserId(),
                    paymentDate = paymentLogs.single().getPaymentDate(),
                    paymentType = paymentLogs.single().getPaymentType(),
                    totalAmount = paymentLogs.single().getTotalAmount(),
                    pointTransaction = null,
                    cardInfo = null,
                    transactionInfo = null,
                    managedEmail = null,
                    eventType = paymentLogs.single().getEventType(),
                ),
            ),
            service.getPaymentHistory(1L),
        )
        assertEquals(
            listOf(
                ChargeLogResult(
                    chargeId = chargeLogs.single().getChargeId(),
                    userId = chargeLogs.single().getUserId(),
                    chargeDate = chargeLogs.single().getChargeDate(),
                    chargeAmount = chargeLogs.single().getChargeAmount(),
                    pointTransaction = PointTransactionResult(
                        beforePoint = 0,
                        transactionPoint = 1000,
                        afterPoint = 1000,
                    ),
                    cardInfo = null,
                    transactionInfo = null,
                    managedEmail = null,
                    reason = null,
                    refundState = RefundState.NONE,
                    refundDate = null,
                    refundRequesterId = null,
                ),
            ),
            service.getChargeHistory(1L),
        )
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
