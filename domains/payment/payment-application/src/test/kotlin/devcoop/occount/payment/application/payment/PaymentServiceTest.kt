package devcoop.occount.payment.application.payment

import devcoop.occount.payment.application.dto.request.ProductInfo
import devcoop.occount.payment.application.dto.response.PgResponse
import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.exception.InsufficientPointsException
import devcoop.occount.payment.domain.exception.InvalidPaymentRequestException
import devcoop.occount.payment.domain.type.CardType
import devcoop.occount.payment.domain.type.PaymentType
import devcoop.occount.payment.domain.type.RefundState
import devcoop.occount.payment.domain.type.TransactionType
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PaymentServiceTest {
    @Test
    fun `charge creates charge log and returns charge response`() {
        val paymentLogRepository = FakePaymentLogRepository()
        val chargeLogRepository = FakeChargeLogRepository()
        val pointWalletPort = FakePointWalletPort(balance = 100)
        val cardPaymentPort = FakeCardPaymentPort()
        val service = PaymentService(
            memberPaymentReadPort = FakeMemberPaymentReadPort(),
            pointWalletPort = pointWalletPort,
            cardPaymentPort = cardPaymentPort,
            paymentLogRepository = paymentLogRepository,
            chargeLogRepository = chargeLogRepository,
        )

        val response = service.execute(
            request = PaymentRequest(
                type = TransactionType.CHARGE,
                charge = ChargeRequest(amount = 50, method = "CARD"),
                payment = null,
            ),
            userId = 1L,
        )

        assertEquals(PaymentType.CARD, response.type)
        assertEquals(150, response.remainingPoints)
        assertEquals(1, chargeLogRepository.saved.size)
        assertEquals(0, paymentLogRepository.saved.size)
        assertEquals(50, cardPaymentPort.approvedAmounts.single())
    }

    @Test
    fun `payment uses points only and stores payment history`() {
        val paymentLogRepository = FakePaymentLogRepository()
        val service = PaymentService(
            memberPaymentReadPort = FakeMemberPaymentReadPort(),
            pointWalletPort = FakePointWalletPort(balance = 120),
            cardPaymentPort = FakeCardPaymentPort(),
            paymentLogRepository = paymentLogRepository,
            chargeLogRepository = FakeChargeLogRepository(),
        )

        val response = service.execute(
            request = paymentRequest(TransactionType.PAYMENT, totalAmount = 80),
            userId = 1L,
        )

        assertEquals(PaymentType.POINT, response.type)
        assertEquals(80, response.pointsUsed)
        assertNull(response.cardAmount)
        assertEquals(40, response.remainingPoints)
        assertEquals(1, paymentLogRepository.saved.size)
    }

    @Test
    fun `payment fails when points are insufficient`() {
        val service = PaymentService(
            memberPaymentReadPort = FakeMemberPaymentReadPort(),
            pointWalletPort = FakePointWalletPort(balance = 30),
            cardPaymentPort = FakeCardPaymentPort(),
            paymentLogRepository = FakePaymentLogRepository(),
            chargeLogRepository = FakeChargeLogRepository(),
        )

        assertFailsWith<InsufficientPointsException> {
            service.execute(paymentRequest(TransactionType.PAYMENT, totalAmount = 80), 1L)
        }
    }

    @Test
    fun `mixed uses points first and charges card for the remainder`() {
        val paymentLogRepository = FakePaymentLogRepository()
        val cardPaymentPort = FakeCardPaymentPort()
        val service = PaymentService(
            memberPaymentReadPort = FakeMemberPaymentReadPort(),
            pointWalletPort = FakePointWalletPort(balance = 30),
            cardPaymentPort = cardPaymentPort,
            paymentLogRepository = paymentLogRepository,
            chargeLogRepository = FakeChargeLogRepository(),
        )

        val response = service.execute(
            request = paymentRequest(TransactionType.MIXED, totalAmount = 80),
            userId = 1L,
        )

        assertEquals(PaymentType.MIXED, response.type)
        assertEquals(30, response.pointsUsed)
        assertEquals(50, response.cardAmount)
        assertEquals(0, response.remainingPoints)
        assertEquals(50, cardPaymentPort.approvedAmounts.single())
        assertEquals(1, paymentLogRepository.saved.size)
    }

    @Test
    fun `mixed rejects request when it is not actually a mixed payment`() {
        val service = PaymentService(
            memberPaymentReadPort = FakeMemberPaymentReadPort(),
            pointWalletPort = FakePointWalletPort(balance = 80),
            cardPaymentPort = FakeCardPaymentPort(),
            paymentLogRepository = FakePaymentLogRepository(),
            chargeLogRepository = FakeChargeLogRepository(),
        )

        assertFailsWith<InvalidPaymentRequestException> {
            service.execute(paymentRequest(TransactionType.MIXED, totalAmount = 80), 1L)
        }
    }

    @Test
    fun `request shape is validated by transaction type`() {
        val service = PaymentService(
            memberPaymentReadPort = FakeMemberPaymentReadPort(),
            pointWalletPort = FakePointWalletPort(balance = 100),
            cardPaymentPort = FakeCardPaymentPort(),
            paymentLogRepository = FakePaymentLogRepository(),
            chargeLogRepository = FakeChargeLogRepository(),
        )

        assertFailsWith<InvalidPaymentRequestException> {
            service.execute(
                PaymentRequest(
                    type = TransactionType.MIXED,
                    charge = ChargeRequest(amount = 10, method = "CARD"),
                    payment = paymentDetails(totalAmount = 20),
                ),
                1L,
            )
        }
    }

    private fun paymentRequest(type: TransactionType, totalAmount: Int): PaymentRequest {
        return PaymentRequest(
            type = type,
            charge = null,
            payment = paymentDetails(totalAmount),
        )
    }

    private fun paymentDetails(totalAmount: Int): PaymentDetails {
        return PaymentDetails(
            items = listOf(
                PaymentItem(
                    itemId = "ITEM-1",
                    itemName = "Coffee",
                    itemPrice = totalAmount,
                    quantity = 1,
                    totalPrice = totalAmount,
                ),
            ),
            totalAmount = totalAmount,
        )
    }

    private class FakeMemberPaymentReadPort : MemberPaymentReadPort {
        override fun getUser(userId: Long): PaymentUserInfo {
            return PaymentUserInfo(userId = userId, email = "user@test.com")
        }
    }

    private class FakePointWalletPort(
        balance: Int,
    ) : PointWalletPort {
        private var currentBalance = balance

        override fun getBalance(userId: Long): Int = currentBalance

        override fun charge(userId: Long, amount: Int): Int {
            currentBalance += amount
            return currentBalance
        }

        override fun deduct(userId: Long, amount: Int): Int {
            currentBalance -= amount
            return currentBalance
        }
    }

    private class FakeCardPaymentPort : CardPaymentPort {
        val approvedAmounts = mutableListOf<Int>()

        override fun approve(amount: Int, products: List<ProductInfo>): PgResponse {
            approvedAmounts += amount
            return PgResponse(
                success = true,
                message = "ok",
                errorCode = null,
                transaction = devcoop.occount.payment.application.dto.response.TransactionInfo(
                    messageNumber = null,
                    typeCode = null,
                    cardNumber = "1234",
                    amount = amount,
                    installmentMonths = 0,
                    cancelType = null,
                    approvalNumber = "APPROVAL-$amount",
                    approvalDate = "20260312",
                    approvalTime = "101010",
                    transactionId = "TX-$amount",
                    terminalId = "TERM-1",
                    merchantNumber = "MERCHANT-1",
                    rejectCode = null,
                    rejectMessage = null,
                ),
                card = devcoop.occount.payment.application.dto.response.CardInfo(
                    acquirerCode = "ACQ",
                    acquirerName = "Acquirer",
                    issuerCode = "ISS",
                    issuerName = "Issuer",
                    cardType = CardType.CREDIT,
                    cardCategory = "PERSONAL",
                    cardName = "Test Card",
                    cardBrand = "VISA",
                ),
                additional = null,
                rawResponse = null,
            )
        }
    }

    private class FakePaymentLogRepository : PaymentLogRepository {
        val saved = mutableListOf<PaymentLog>()

        override fun findByUserId(userId: Long): List<PaymentLog> = saved.filter { it.getUserId() == userId }

        override fun findByUserIdAndPaymentDateBetween(
            userId: Long,
            startDate: LocalDateTime,
            endDate: LocalDateTime,
        ): List<PaymentLog> = saved

        override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> =
            saved.filter { it.getPaymentType() == paymentType }

        override fun save(paymentLog: PaymentLog): PaymentLog {
            saved += paymentLog
            return paymentLog
        }

        override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> {
            saved += paymentLogs
            return paymentLogs
        }
    }

    private class FakeChargeLogRepository : ChargeLogRepository {
        val saved = mutableListOf<ChargeLog>()

        override fun findByUserId(userId: Long): List<ChargeLog> = saved.filter { it.getUserId() == userId }

        override fun findByPaymentId(paymentId: String): ChargeLog? = null

        override fun findByRefundState(refundState: RefundState): List<ChargeLog> = emptyList()

        override fun findByUserIdAndChargeDateBetween(
            userId: Long,
            startDate: LocalDateTime,
            endDate: LocalDateTime,
        ): List<ChargeLog> = saved

        override fun save(chargeLog: ChargeLog): ChargeLog {
            saved += chargeLog
            return chargeLog
        }

        override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> {
            saved += chargeLogs
            return chargeLogs
        }
    }
}
