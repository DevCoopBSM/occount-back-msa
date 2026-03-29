package devcoop.occount.payment.api.payment

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.payment.api.dto.request.ChargeRequest
import devcoop.occount.payment.application.query.paymentlog.GetPaymentHistoryQueryService
import devcoop.occount.payment.application.query.paymentlog.PaymentLogResult
import devcoop.occount.payment.application.shared.PaymentFacade
import devcoop.occount.payment.application.shared.PaymentRequest
import devcoop.occount.payment.application.shared.PaymentResponse
import devcoop.occount.payment.application.usecase.charge.CardChargeUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
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
    private val cardChargeUseCase: CardChargeUseCase,
    private val getPaymentHistoryQueryService: GetPaymentHistoryQueryService,
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

    @PostMapping("/charge")
    @ResponseStatus(HttpStatus.OK)
    fun charge(
        @Valid @RequestBody request: ChargeRequest,
        httpRequest: HttpServletRequest,
    ): PaymentResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return cardChargeUseCase.execute(userId, request.amount)
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
