package devcoop.occount.payment.api.payment

import devcoop.occount.core.common.auth.AuthPrincipal
import devcoop.occount.core.common.auth.AuthUser
import devcoop.occount.payment.application.query.paymentlog.GetPaymentHistoryQueryService
import devcoop.occount.payment.application.query.paymentlog.PaymentHistoryListResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * 로그인 사용자 본인의 결제 내역 조회 API.
 * 인가는 게이트웨이의 payments 하위 경로 규칙(AUTHENTICATED)에서 강제한다.
 */
@RestController
@RequestMapping("/payments")
class PaymentController(
    private val getPaymentHistoryQueryService: GetPaymentHistoryQueryService,
) {
    companion object {
        private const val MAX_PAGE_SIZE = 100
        private val ALLOWED_SORT_FIELDS = setOf("paymentId", "paymentDate")
        private val DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "paymentDate")
    }

    @GetMapping("/history")
    fun getPaymentHistory(
        @AuthUser principal: AuthPrincipal,
        @PageableDefault(size = 10, sort = ["paymentDate"], direction = Sort.Direction.DESC) pageable: Pageable,
        @RequestParam(name = "start_date", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(name = "end_date", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?,
    ): PaymentHistoryListResponse {
        val sanitized = sanitizePageable(pageable)
        return if (startDate != null && endDate != null) {
            getPaymentHistoryQueryService.getPaymentHistoryByDateRange(
                userId = principal.userId,
                startDate = startDate,
                endDate = endDate,
                pageable = sanitized,
            )
        } else {
            getPaymentHistoryQueryService.getPaymentHistory(principal.userId, sanitized)
        }
    }

    private fun sanitizePageable(pageable: Pageable): Pageable {
        val cappedSize = minOf(pageable.pageSize, MAX_PAGE_SIZE)
        val filteredOrders = pageable.sort.filter { it.property in ALLOWED_SORT_FIELDS }.toList()
        val sanitizedSort = if (filteredOrders.isEmpty()) DEFAULT_SORT else Sort.by(filteredOrders)
        return PageRequest.of(pageable.pageNumber, cappedSize, sanitizedSort)
    }
}
