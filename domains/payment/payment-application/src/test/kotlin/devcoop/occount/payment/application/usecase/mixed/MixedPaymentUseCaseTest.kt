package devcoop.occount.payment.application.usecase.mixed

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.VanResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.query.wallet.GetWalletPointQueryService
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentItem
import devcoop.occount.payment.application.support.FakeWalletRepository
import devcoop.occount.payment.application.usecase.wallet.deduct.DeductWalletUseCase
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.wallet.Wallet
import devcoop.occount.payment.application.exception.InvalidPaymentRequestException
import devcoop.occount.payment.application.usecase.payment.MixedPaymentUseCase
import devcoop.occount.payment.domain.payment.CardType
import devcoop.occount.payment.domain.payment.PaymentType
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MixedPaymentUseCaseTest {
    @Test
    fun `mixed uses points first and charges card for the remainder`() {
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 30)),
        )
        val paymentLogRepository = FakePaymentLogRepository()
        val cardPaymentPort = FakeCardPaymentPort()
        val useCase = MixedPaymentUseCase(
            getWalletPointQueryService = GetWalletPointQueryService(walletRepository),
            deductWalletUseCase = DeductWalletUseCase(walletRepository),
            cardPaymentPort = cardPaymentPort,
            paymentLogRepository = paymentLogRepository,
        )

        val response = useCase.execute(
            userId = 1L,
            kioskId = "kiosk-1",
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
        val walletRepository = FakeWalletRepository(
            wallets = mutableMapOf(1L to Wallet(userId = 1L, point = 80)),
        )
        val useCase = MixedPaymentUseCase(
            getWalletPointQueryService = GetWalletPointQueryService(walletRepository),
            deductWalletUseCase = DeductWalletUseCase(walletRepository),
            cardPaymentPort = FakeCardPaymentPort(),
            paymentLogRepository = FakePaymentLogRepository(),
        )

        assertFailsWith<InvalidPaymentRequestException> {
            useCase.execute(userId = 1L, kioskId = "kiosk-1", details = paymentDetails(totalAmount = 80))
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

    private class FakeCardPaymentPort : CardPaymentPort {
        val approvedAmounts = mutableListOf<Int>()

        override fun approve(amount: Int, items: List<ItemCommand>, kioskId: String, paymentKey: String?): VanResult {
            approvedAmounts += amount
            return VanResult(
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

        override fun refund(transactionId: String?, approvalNumber: String?, approvalDate: String, terminalId: String?, amount: Int, kioskId: String): VanResult {
            error("not used in this test")
        }

        override fun requestPendingApprovalCancellation(paymentKey: String, kioskId: String) {
            error("not used in this test")
        }
    }

    private class FakePaymentLogRepository : PaymentLogRepository {
        val saved = mutableListOf<PaymentLog>()

        override fun findById(paymentId: Long): PaymentLog? = saved.firstOrNull { it.getPaymentId() == paymentId }
        override fun findByUserId(userId: Long): List<PaymentLog> = saved.filter { it.getUserId() == userId }
        override fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentLog> = saved
        override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> = saved.filter { it.getPaymentType() == paymentType }
        override fun save(paymentLog: PaymentLog): PaymentLog { saved += paymentLog; return paymentLog }
        override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> { saved += paymentLogs; return paymentLogs }
    }

}
