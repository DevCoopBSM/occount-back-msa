package devcoop.occount.payment.api.payment

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.payment.application.payment.ChargeLogResult
import devcoop.occount.payment.application.payment.PaymentRequest
import devcoop.occount.payment.application.payment.PaymentLogResult
import devcoop.occount.payment.application.payment.PaymentResponse
import devcoop.occount.payment.application.payment.PaymentService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/payments")
class PaymentController(
    private val paymentService: PaymentService,
) {
    @PostMapping("/execute")
    @ResponseStatus(HttpStatus.OK)
    fun executePayment(
        @RequestBody request: PaymentRequest,
        httpRequest: HttpServletRequest,
    ): PaymentResponse {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return paymentService.execute(request, authPrincipal.userId)
    }

    @GetMapping("/history")
    @ResponseStatus(HttpStatus.OK)
    fun getPaymentHistory(httpRequest: HttpServletRequest): List<PaymentLogResult> {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return paymentService.getPaymentHistory(authPrincipal.userId)
    }

    @GetMapping("/history/range")
    @ResponseStatus(HttpStatus.OK)
    fun getPaymentHistoryByDateRange(
        httpRequest: HttpServletRequest,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime,
    ): List<PaymentLogResult> {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return paymentService.getPaymentHistoryByDateRange(authPrincipal.userId, startDate, endDate)
    }

    @GetMapping("/charges")
    @ResponseStatus(HttpStatus.OK)
    fun getChargeHistory(httpRequest: HttpServletRequest): List<ChargeLogResult> {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return paymentService.getChargeHistory(authPrincipal.userId)
    }

    @GetMapping("/charges/range")
    @ResponseStatus(HttpStatus.OK)
    fun getChargeHistoryByDateRange(
        httpRequest: HttpServletRequest,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime,
    ): List<ChargeLogResult> {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return paymentService.getChargeHistoryByDateRange(authPrincipal.userId, startDate, endDate)
    }
}
