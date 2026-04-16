package devcoop.occount.payment.api.payment

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.payment.application.query.paymentlog.GetPaymentHistoryQueryService
import devcoop.occount.payment.application.query.paymentlog.PaymentLogResult
import devcoop.occount.payment.application.shared.PaymentFacade
import devcoop.occount.payment.application.shared.PaymentRequest
import devcoop.occount.payment.application.shared.PaymentResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/payments")
class PaymentController(
    private val paymentFacade: PaymentFacade,
    private val getPaymentHistoryQueryService: GetPaymentHistoryQueryService,
) {
    @PostMapping("/execute")
    @ResponseStatus(HttpStatus.OK)
    fun executePayment(
        @RequestBody request: PaymentRequest,
        httpRequest: HttpServletRequest,
    ): PaymentResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return paymentFacade.execute(userId, request.requirePayment())
    }

    @GetMapping("/history")
    @ResponseStatus(HttpStatus.OK)
    fun getPaymentHistory(httpRequest: HttpServletRequest): List<PaymentLogResult> {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getPaymentHistoryQueryService.getPaymentHistory(userId)
    }

    @GetMapping("/history/range")
    @ResponseStatus(HttpStatus.OK)
    fun getPaymentHistoryByDateRange(
        httpRequest: HttpServletRequest,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime,
    ): List<PaymentLogResult> {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getPaymentHistoryQueryService.getPaymentHistoryByDateRange(userId, startDate, endDate)
    }
}
