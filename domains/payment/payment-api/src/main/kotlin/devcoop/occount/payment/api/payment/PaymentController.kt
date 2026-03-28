package devcoop.occount.payment.api.payment

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.payment.application.query.chargelog.ChargeLogResult
import devcoop.occount.payment.application.query.chargelog.GetChargeHistoryQueryService
import devcoop.occount.payment.application.query.paymentlog.GetPaymentHistoryQueryService
import devcoop.occount.payment.application.query.paymentlog.PaymentLogResult
import devcoop.occount.payment.application.shared.PaymentFacade
import devcoop.occount.payment.application.shared.PaymentRequest
import devcoop.occount.payment.application.shared.PaymentResponse
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
    private val paymentFacade: PaymentFacade,
    private val getPaymentHistoryQueryService: GetPaymentHistoryQueryService,
    private val getChargeHistoryQueryService: GetChargeHistoryQueryService,
) {
    @PostMapping("/execute")
    @ResponseStatus(HttpStatus.OK)
    fun executePayment(
        @RequestBody request: PaymentRequest,
        httpRequest: HttpServletRequest,
    ): PaymentResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return paymentFacade.execute(userId, request)
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

    @GetMapping("/charges")
    @ResponseStatus(HttpStatus.OK)
    fun getChargeHistory(httpRequest: HttpServletRequest): List<ChargeLogResult> {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getChargeHistoryQueryService.getChargeHistory(userId)
    }

    @GetMapping("/charges/range")
    @ResponseStatus(HttpStatus.OK)
    fun getChargeHistoryByDateRange(
        httpRequest: HttpServletRequest,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime,
    ): List<ChargeLogResult> {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getChargeHistoryQueryService.getChargeHistoryByDateRange(userId, startDate, endDate)
    }
}
