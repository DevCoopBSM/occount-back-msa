package devcoop.occount.payment.application.usecase.charge

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.PgResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.MemberPaymentReadPort
import devcoop.occount.payment.application.output.PointWalletPort
import devcoop.occount.payment.application.shared.ChargeRequest
import devcoop.occount.payment.application.shared.PaymentUserInfo
import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.ChargeLogRepository
import devcoop.occount.payment.domain.type.CardType
import devcoop.occount.payment.domain.type.PaymentType
import devcoop.occount.payment.domain.type.RefundState
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ChargeUseCaseTest {
    @Test
    fun `charge creates charge log and returns charge response`() {
        val chargeLogRepository = FakeChargeLogRepository()
        val pointWalletPort = FakePointWalletPort(balance = 100)
        val cardPaymentPort = FakeCardPaymentPort()
        val useCase = ChargeUseCase(
            memberPaymentReadPort = FakeMemberPaymentReadPort(),
            pointWalletPort = pointWalletPort,
            cardPaymentPort = cardPaymentPort,
            chargeLogRepository = chargeLogRepository,
        )

        val response = useCase.execute(
            userId = 1L,
            request = ChargeRequest(amount = 50, method = "CARD"),
        )

        assertEquals(PaymentType.CARD, response.type)
        assertEquals(150, response.remainingPoints)
        assertEquals(1, chargeLogRepository.saved.size)
        assertEquals(50, cardPaymentPort.approvedAmounts.single())
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

    private class FakeChargeLogRepository : ChargeLogRepository {
        val saved = mutableListOf<ChargeLog>()

        override fun findByUserId(userId: Long): List<ChargeLog> = saved.filter { it.getUserId() == userId }
        override fun findByPaymentId(paymentId: String): ChargeLog? = null
        override fun findByRefundState(refundState: RefundState): List<ChargeLog> = emptyList()
        override fun findByUserIdAndChargeDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<ChargeLog> = saved
        override fun save(chargeLog: ChargeLog): ChargeLog { saved += chargeLog; return chargeLog }
        override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> { saved += chargeLogs; return chargeLogs }
    }
}
