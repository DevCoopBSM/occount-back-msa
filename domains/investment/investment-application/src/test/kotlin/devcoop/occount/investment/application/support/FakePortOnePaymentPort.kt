package devcoop.occount.investment.application.support

import devcoop.occount.investment.application.exception.PortOnePaymentLookupException
import devcoop.occount.investment.application.output.PortOnePayment
import devcoop.occount.investment.application.output.PortOnePaymentPort

class FakePortOnePaymentPort(
    private val payments: Map<String, PortOnePayment> = emptyMap(),
) : PortOnePaymentPort {
    override fun fetchPayment(paymentId: String): PortOnePayment =
        payments[paymentId] ?: throw PortOnePaymentLookupException()
}
