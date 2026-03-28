package devcoop.occount.payment.application.usecase.mixed

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.PgResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.MemberPaymentReadPort
import devcoop.occount.payment.application.output.PointWalletPort
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentItem
import devcoop.occount.payment.application.shared.PaymentUserInfo
import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.PaymentLogRepository
import devcoop.occount.payment.domain.exception.InvalidPaymentRequestException
import devcoop.occount.payment.domain.type.CardType
import devcoop.occount.payment.domain.type.PaymentType
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MixedPaymentUseCaseTest {
    @Test
    fun `mixed uses points first and charges card for the remainder`() {
        val paymentLogRepository = FakePaymentLogRepository()
        val cardPaymentPort = FakeCardPaymentPort()
        val useCase = MixedPaymentUseCase(
            memberPaymentReadPort = FakeMemberPaymentReadPort(),
            pointWalletPort = FakePointWalletPort(balance = 30),
            cardPaymentPort = cardPaymentPort,
            paymentLogRepository = paymentLogRepository,
        )

        val response = useCase.execute(
            userId = 1L,
            details = paymentDetails(totalAmount = 80),
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
        val useCase = MixedPaymentUseCase(
            memberPaymentReadPort = FakeMemberPaymentReadPort(),
            pointWalletPort = FakePointWalletPort(balance = 80),
            cardPaymentPort = FakeCardPaymentPort(),
            paymentLogRepository = FakePaymentLogRepository(),
        )

        assertFailsWith<InvalidPaymentRequestException> {
            useCase.execute(userId = 1L, details = paymentDetails(totalAmount = 80))
        }
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

    private class FakePointWalletPort(balance: Int) : PointWalletPort {
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

        override fun approve(amount: Int, items: List<ItemCommand>): PgResult {
            approvedAmounts += amount
            return PgResult(
                success = true,
                message = "ok",
                errorCode = null,
                transaction = TransactionResult(
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
                card = CardResult(
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
        override fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentLog> = saved
        override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> = saved.filter { it.getPaymentType() == paymentType }
        override fun save(paymentLog: PaymentLog): PaymentLog { saved += paymentLog; return paymentLog }
        override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> { saved += paymentLogs; return paymentLogs }
    }
}
